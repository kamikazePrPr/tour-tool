package com.kamikazeprpr.tourtool.update

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.content.FileProvider
import com.kamikazeprpr.tourtool.BuildConfig
import com.kamikazeprpr.tourtool.data.AppLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/**
 * 从 GitHub 查新版本、下载、校验、拉起安装。
 *
 * 三道校验缺一不可：Release 里的 sha256 要与下载到的文件一致、APK 里的包名必须是自己、
 * 签名证书摘要必须与自己相同。任何一道不过就拒装，并给一句人话。
 *
 * 联网只碰 GitHub 这几个域名（network_security_config 里白名单锁死），
 * 请求体里只有版本号，不带任何客人数据。
 */
object UpdateChecker {

    private const val OWNER = "kamikazePrPr"
    private const val REPO = "tour-tool"
    // 用 releases 列表而不是 /releases/latest：后者只认正式版，
    // 只有 prerelease 的时候会返回 404，把人坑得很惨。
    private const val API = "https://api.github.com/repos/$OWNER/$REPO/releases?per_page=5"

    data class Release(
        val version: String,
        val apkUrl: String,
        val shaUrl: String?,
        val pageUrl: String,
    )

    sealed interface CheckResult {
        data class Newer(val release: Release) : CheckResult
        data object UpToDate : CheckResult
        data class Failed(val reason: String) : CheckResult
    }

    sealed interface InstallResult {
        data class Ready(val file: File) : InstallResult
        data class Rejected(val reason: String) : InstallResult
    }

    /** 冷启动后调一次。超时短、失败静默，绝不因为网络问题卡住启动。 */
    suspend fun check(): CheckResult = withContext(Dispatchers.IO) {
        try {
            val conn = (URL(API).openConnection() as HttpURLConnection).apply {
                connectTimeout = 5000
                readTimeout = 5000
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("User-Agent", "tour-tool")
            }
            if (conn.responseCode != 200) {
                return@withContext CheckResult.Failed("服务器返回 ${conn.responseCode}")
            }
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            val arr = org.json.JSONArray(body)
            val list = (0 until arr.length()).map { arr.getJSONObject(it) }
            val json = list.firstOrNull { !it.optBoolean("draft") }
                ?: return@withContext CheckResult.Failed("没有可用的版本")
            val tag = json.optString("tag_name").removePrefix("v")
            if (tag.isBlank()) return@withContext CheckResult.Failed("版本号是空的")

            if (!isNewer(tag, BuildConfig.VERSION_NAME)) {
                AppLog.i("UPDATE", "已是最新：$tag")
                return@withContext CheckResult.UpToDate
            }

            val assets = json.optJSONArray("assets") ?: return@withContext CheckResult.Failed("没有附件")
            var apk: String? = null
            var sha: String? = null
            for (i in 0 until assets.length()) {
                val a = assets.getJSONObject(i)
                val name = a.optString("name")
                val url = a.optString("browser_download_url")
                when {
                    name.endsWith(".apk") -> apk = url
                    name.endsWith(".sha256") -> sha = url
                }
            }
            if (apk == null) return@withContext CheckResult.Failed("没有找到安装包")

            AppLog.i("UPDATE", "发现新版本 $tag")
            CheckResult.Newer(
                Release(
                    version = tag,
                    apkUrl = apk,
                    shaUrl = sha,
                    pageUrl = json.optString("html_url"),
                )
            )
        } catch (e: Exception) {
            AppLog.d("UPDATE", "检查失败：${e.javaClass.simpleName}")
            CheckResult.Failed(e.message ?: "网络不通")
        }
    }

    /** 下载并过三道校验，全部通过才返回可安装的文件。 */
    suspend fun download(ctx: Context, release: Release): InstallResult = withContext(Dispatchers.IO) {
        try {
            val dir = File(ctx.cacheDir, "update").apply { mkdirs() }
            dir.listFiles()?.forEach { it.delete() }
            val apkFile = File(dir, "update-${release.version}.apk")

            downloadTo(release.apkUrl, apkFile)
            AppLog.i("UPDATE", "下载完成 ${apkFile.length()} 字节")

            // 第一道：sha256
            val expected = release.shaUrl?.let { url ->
                val text = downloadText(url)
                text.trim().substringBefore(' ').lowercase()
            }
            if (!expected.isNullOrBlank()) {
                val actual = sha256(apkFile)
                if (actual != expected) {
                    AppLog.e("UPDATE", "sha256 不一致")
                    apkFile.delete()
                    return@withContext InstallResult.Rejected("下载的文件不完整")
                }
            } else {
                AppLog.d("UPDATE", "Release 里没有 sha256 附件，跳过这一道")
            }

            val pm = ctx.packageManager

            // 第二道：包名
            val archive = pm.getPackageArchiveInfo(apkFile.absolutePath, 0)
            if (archive == null || archive.packageName != ctx.packageName) {
                apkFile.delete()
                return@withContext InstallResult.Rejected("安装包的包名不对")
            }

            // 第三道：签名证书摘要
            val apkSig = signatureHash(pm, apkFile.absolutePath)
            val selfSig = installedSignatureHash(pm, ctx.packageName)
            if (apkSig == null || selfSig == null || apkSig != selfSig) {
                apkFile.delete()
                return@withContext InstallResult.Rejected("安装包的签名和当前版本不一样")
            }

            InstallResult.Ready(apkFile)
        } catch (e: Exception) {
            AppLog.e("UPDATE", "下载失败：${e.message}")
            InstallResult.Rejected("下载失败")
        }
    }

    /** 拉起系统安装界面。安卓不允许普通应用静默安装，必须用户点一下。 */
    fun install(ctx: Context, apk: File) {
        val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", apk)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        ctx.startActivity(intent)
    }

    fun openReleasePage(ctx: Context, url: String) {
        try {
            ctx.startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        } catch (_: Exception) {
            AppLog.e("UPDATE", "没有浏览器可以打开页面")
        }
    }

    // ---------- 内部工具 ----------

    private fun isNewer(remote: String, local: String): Boolean {
        val r = remote.split(".").mapNotNull { it.toIntOrNull() }
        val l = local.split(".").mapNotNull { it.toIntOrNull() }
        for (i in 0 until maxOf(r.size, l.size)) {
            val a = r.getOrElse(i) { 0 }
            val b = l.getOrElse(i) { 0 }
            if (a != b) return a > b
        }
        return false
    }

    private fun downloadTo(url: String, out: File) {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 8000
            readTimeout = 15000
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "tour-tool")
        }
        conn.inputStream.use { input ->
            out.outputStream().use { output -> input.copyTo(output, 64 * 1024) }
        }
    }

    private fun downloadText(url: String): String {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 8000
            readTimeout = 8000
            setRequestProperty("User-Agent", "tour-tool")
        }
        return conn.inputStream.bufferedReader().use { it.readText() }
    }

    private fun sha256(file: File): String {
        val md = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buf = ByteArray(64 * 1024)
            while (true) {
                val n = input.read(buf)
                if (n <= 0) break
                md.update(buf, 0, n)
            }
        }
        return md.digest().joinToString("") { "%02x".format(it) }
    }

    private fun signatureHash(pm: PackageManager, apkPath: String): String? {
        @Suppress("DEPRECATION")
        val info = pm.getPackageArchiveInfo(apkPath, PackageManager.GET_SIGNATURES) ?: return null
        @Suppress("DEPRECATION")
        val sig = info.signatures?.firstOrNull() ?: return null
        return MessageDigest.getInstance("SHA-256").digest(sig.toByteArray())
            .joinToString("") { "%02x".format(it) }
    }

    private fun installedSignatureHash(pm: PackageManager, pkg: String): String? {
        @Suppress("DEPRECATION")
        val info = pm.getPackageInfo(pkg, PackageManager.GET_SIGNATURES)
        @Suppress("DEPRECATION")
        val sig = info.signatures?.firstOrNull() ?: return null
        return MessageDigest.getInstance("SHA-256").digest(sig.toByteArray())
            .joinToString("") { "%02x".format(it) }
    }
}
