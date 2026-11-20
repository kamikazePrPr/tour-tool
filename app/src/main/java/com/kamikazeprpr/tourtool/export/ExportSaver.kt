package com.kamikazeprpr.tourtool.export

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.kamikazeprpr.tourtool.data.AppLog
import java.io.File

/**
 * 把导出的表格存到「下载」文件夹，让用户能在文件管理器里直接找到。
 *
 * Android 10 起走 MediaStore.Downloads，不需要任何存储权限；
 * 更老的系统只能写公共目录，那时候要 WRITE_EXTERNAL_STORAGE。
 * 返回一句人话，直接拿去显示在界面上。
 */
object ExportSaver {

    data class Result(val ok: Boolean, val message: String)

    fun save(ctx: Context, file: File, mimeType: String): Result {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                saveViaMediaStore(ctx, file, mimeType)
            } else {
                saveToLegacyDownloads(file)
            }
        } catch (e: Exception) {
            AppLog.e("EXPORT", "存到本地失败：${e.javaClass.simpleName} ${e.message}")
            Result(false, "没存上：${e.message ?: "未知错误"}")
        }
    }

    private fun saveViaMediaStore(ctx: Context, file: File, mimeType: String): Result {
        val resolver = ctx.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, file.name)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            ?: return Result(false, "系统没给存文件的位置")

        resolver.openOutputStream(uri)?.use { out ->
            file.inputStream().use { input -> input.copyTo(out) }
        } ?: return Result(false, "文件写不进去")

        // 写完再把 pending 放开，别的应用这时候才能看到这个文件
        values.clear()
        values.put(MediaStore.MediaColumns.IS_PENDING, 0)
        resolver.update(uri, values, null, null)

        AppLog.i("EXPORT", "已存到下载目录：${file.name}")
        return Result(true, "已经存到「下载」文件夹：${file.name}")
    }

    @Suppress("DEPRECATION")
    private fun saveToLegacyDownloads(file: File): Result {
        val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (!dir.exists() && !dir.mkdirs()) {
            return Result(false, "建不了「下载」文件夹")
        }
        val target = File(dir, file.name)
        file.copyTo(target, overwrite = true)
        AppLog.i("EXPORT", "已存到下载目录（旧系统）：${file.name}")
        return Result(true, "已经存到「下载」文件夹：${file.name}")
    }
}
