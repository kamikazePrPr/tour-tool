package com.kamikazeprpr.tourtool.data

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * 本机运行日志。
 *
 * 三条硬规矩：
 * 1. 绝不写姓名和完整身份证号——号码只留前 3 后 2（110******34），
 *    日志本身就是数据，不脱敏等于在手机上又存了一份客人名单；
 * 2. 按天一个文件，单文件超 2 MB 就切下一个，默认只留 7 天；
 * 3. 全部存在应用私有目录，清空团数据时一起清掉。
 */
object AppLog {

    private const val DIR = "logs"
    private const val MAX_BYTES = 2L * 1024 * 1024
    private const val KEEP_DAYS = 7

    private val dateFmt = SimpleDateFormat("yyyyMMdd", Locale.CHINA)
    private val timeFmt = SimpleDateFormat("MM-dd HH:mm:ss.SSS", Locale.CHINA)
    private var appContext: Context? = null

    /**
     * 日志一律丢给这个单线程队列写盘。
     * 原来是在调用线程里直接 appendText——识别时每张图要写四行，
     * 而调用方在主线程，等于每张图都卡一下磁盘 IO。
     */
    private val writer = java.util.concurrent.Executors.newSingleThreadExecutor { r ->
        Thread(r, "app-log-writer").apply { isDaemon = true }
    }

    /** 崩溃时用：把队列里剩的写完再返回，不然进程一死日志就丢了 */
    fun flushBlocking(timeoutMs: Long = 400) {
        try {
            writer.shutdown()
            writer.awaitTermination(timeoutMs, java.util.concurrent.TimeUnit.MILLISECONDS)
        } catch (_: Throwable) {
        }
    }

    fun init(ctx: Context) {
        appContext = ctx.applicationContext
        cleanup(ctx)
        val handler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                e("CRASH", "线程 ${thread.name} 崩了：${throwable.javaClass.simpleName}: ${throwable.message}")
                throwable.stackTrace.take(30).forEach { e("CRASH", "  at $it") }
                flushBlocking()
            } catch (_: Throwable) {
                // 写日志本身失败就算了，不能盖住原始崩溃
            }
            handler?.uncaughtException(thread, throwable)
        }
        i("APP", "启动")
    }

    fun i(tag: String, msg: String) = write("I", tag, msg)
    fun d(tag: String, msg: String) = write("D", tag, msg)
    fun e(tag: String, msg: String) = write("E", tag, msg)

    /** 身份证号脱敏：只留前 3 位和后 2 位 */
    fun maskId(id: String): String {
        if (id.length < 8) return "***"
        return id.take(3) + "*".repeat(id.length - 5) + id.takeLast(2)
    }

    /** 把一段文本里所有 18 位号码都换成掩码 */
    fun maskAll(text: String): String =
        Regex("""\d{17}[0-9Xx]""").replace(text) { maskId(it.value) }

    @Synchronized
    private fun write(level: String, tag: String, msg: String) {
        val ctx = appContext ?: return
        val line = "${timeFmt.format(Date())} $level/$tag: $msg\n"
        try {
            writer.execute {
                try {
                    val dir = File(ctx.filesDir, DIR).apply { mkdirs() }
                    val day = dateFmt.format(Date())
                    var file = File(dir, "app-$day.log")
                    var index = 1
                    while (file.length() > MAX_BYTES) {
                        file = File(dir, "app-$day-$index.log")
                        index++
                        if (index > 20) break
                    }
                    file.appendText(line, Charsets.UTF_8)
                } catch (_: Throwable) {
                    // 日志失败绝不能影响主流程
                }
            }
        } catch (_: Throwable) {
            // 队列被关掉（进程要退出了）就静默丢弃
        }
    }

    /** 删掉超过 7 天的日志，启动时和清空数据时各跑一次 */
    fun cleanup(ctx: Context) {
        try {
            val dir = File(ctx.filesDir, DIR)
            if (!dir.exists()) return
            val cutoff = System.currentTimeMillis() - KEEP_DAYS * 24L * 3600 * 1000
            dir.listFiles()?.forEach { f ->
                if (f.isFile && f.lastModified() < cutoff) f.delete()
            }
        } catch (_: Throwable) {
        }
    }

    fun clearAll(ctx: Context) {
        try {
            File(ctx.filesDir, DIR).listFiles()?.forEach { it.delete() }
        } catch (_: Throwable) {
        }
    }

    fun logFiles(ctx: Context): List<File> =
        File(ctx.filesDir, DIR).listFiles()?.sortedByDescending { it.name }.orEmpty()

    /** 把日志打成一个 zip，走系统分享发给开发者。包里只有日志，没有任何客人数据。 */
    fun exportBundle(ctx: Context, out: File) {
        val files = logFiles(ctx)
        ZipOutputStream(out.outputStream()).use { zip ->
            files.forEach { f ->
                zip.putNextEntry(ZipEntry(f.name))
                zip.write(f.readBytes())
                zip.closeEntry()
            }
            zip.putNextEntry(ZipEntry("device.txt"))
            val info = buildString {
                appendLine("app=${ctx.packageName}")
                appendLine("time=${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA).format(Date())}")
                appendLine("android=${android.os.Build.VERSION.RELEASE} (API ${android.os.Build.VERSION.SDK_INT})")
                appendLine("model=${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}")
            }
            zip.write(info.toByteArray(Charsets.UTF_8))
            zip.closeEntry()
        }
    }
}
