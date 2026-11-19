package com.kamikazeprpr.tourtool.ocr

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * 三个通道各读一遍。
 *
 * 同一张图用同一个引擎连跑三次结果会一模一样，所以三次必须是三种不同的输入：
 *  A 原图缩小（正常拍摄，求最好那次）
 *  B 灰度 + 拉对比度（对付塑料卡反光和贴膜）
 *  C 轻微旋转（对付手持歪、对焦软）
 */
class OcrEngine {

    private val recognizer =
        TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())

    suspend fun readThreeChannels(source: Bitmap): List<String> = coroutineScope {
        val variants = buildVariants(source)
        try {
            variants.map { bmp -> async { recognize(bmp) } }.awaitAll()
        } finally {
            variants.forEach { v -> if (v !== source && !v.isRecycled) v.recycle() }
        }
    }

    private fun buildVariants(src: Bitmap): List<Bitmap> {
        val a = BitmapOps.scaleToMax(src, MAX_SIDE)
        val b = BitmapOps.grayscaleHighContrast(a)
        // 通道 C：只取卡片上半部分（姓名/性别/出生）放大，专治蓝色小标签读不清。
        // 号码交给 A、B 两个整图通道，实测 2400 分辨率下号码基本一次读对。
        val c = BitmapOps.cropZoom(src, topFraction = 0f, bottomFraction = 0.55f, maxSide = ROI_SIDE)
        return listOf(a, b, c)
    }

    private suspend fun recognize(bmp: Bitmap): String =
        suspendCancellableCoroutine { cont ->
            val image = InputImage.fromBitmap(bmp, 0)
            recognizer.process(image)
                .addOnSuccessListener { result ->
                    if (cont.isActive) cont.resume(result.text)
                }
                .addOnFailureListener {
                    if (cont.isActive) cont.resume("")
                }
        }

    companion object {
        /**
         * 识别前把长边压到 2400。
         * 之前用 1600，实测把号码数字压到二十几个像素高，正好卡在识别边缘；
         * 抬到 2400 后数字高度回到三十多像素，明显更容易读对。
         */
        const val MAX_SIDE = 2400

        /** 专门读号码的下半部分裁片，放大到这个长边 */
        const val ROI_SIDE = 2000
    }
}

/** 图片预处理小工具，全部返回新 Bitmap，调用方负责回收。 */
object BitmapOps {

    fun scaleToMax(src: Bitmap, maxSide: Int): Bitmap {
        val longer = maxOf(src.width, src.height)
        if (longer <= maxSide) return src.copy(Bitmap.Config.ARGB_8888, false)
        val scale = maxSide.toFloat() / longer
        val w = (src.width * scale).toInt().coerceAtLeast(1)
        val h = (src.height * scale).toInt().coerceAtLeast(1)
        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        canvas.drawBitmap(src, null, android.graphics.Rect(0, 0, w, h), Paint(Paint.FILTER_BITMAP_FLAG))
        return out
    }

    /** 去饱和 + 提高对比度，专门对付反光与低对比。 */
    fun grayscaleHighContrast(src: Bitmap): Bitmap {
        val out = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)

        val saturation = ColorMatrix().apply { setSaturation(0f) }
        // 对比度 1.5，围绕中灰 128 拉伸
        val contrast = 1.5f
        val translate = (-0.5f * contrast + 0.5f) * 255f
        val contrastMatrix = ColorMatrix(
            floatArrayOf(
                contrast, 0f, 0f, 0f, translate,
                0f, contrast, 0f, 0f, translate,
                0f, 0f, contrast, 0f, translate,
                0f, 0f, 0f, 1f, 0f,
            )
        )
        saturation.postConcat(contrastMatrix)
        paint.colorFilter = ColorMatrixColorFilter(saturation)
        canvas.drawBitmap(src, 0f, 0f, paint)
        return out
    }

    fun rotate(src: Bitmap, degrees: Float): Bitmap {
        val m = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(src, 0, 0, src.width, src.height, m, true)
    }

    /**
     * 取图片下部一段（身份证的号码在卡片下方），把这块放大到 maxSide。
     * 这比给整张图做超分有效：不做任何"插值猜笔画"，只是把本来就在原图里的像素按比例放大。
     */
    fun cropZoom(src: Bitmap, topFraction: Float, bottomFraction: Float, maxSide: Int): Bitmap {
        val startY = (src.height * topFraction).toInt().coerceIn(0, src.height - 1)
        val endY = (src.height * bottomFraction).toInt().coerceIn(startY + 1, src.height)
        val cropped = Bitmap.createBitmap(src, 0, startY, src.width, endY - startY)
        return try {
            scaleToMax(cropped, maxSide)
        } finally {
            if (!cropped.isRecycled) cropped.recycle()
        }
    }
}
