package com.kamikazeprpr.tourtool.ocr

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri

/**
 * 从相册 URI 按需采样解码：先量尺寸，再算 inSampleSize，绝不把四千万像素原图整个读进内存。
 */
object ImageLoader {

    fun decodeSampled(resolver: ContentResolver, uri: Uri, maxSide: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        try {
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        } catch (_: Exception) {
            return null
        }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        var sample = 1
        while (maxOf(bounds.outWidth / sample, bounds.outHeight / sample) > maxSide) {
            sample *= 2
        }

        val opts = BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        return try {
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
        } catch (_: Exception) {
            null
        }
    }
}
