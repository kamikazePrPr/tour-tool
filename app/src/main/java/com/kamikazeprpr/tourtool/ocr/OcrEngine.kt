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
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.Closeable
import kotlin.coroutines.resume

/**
 * 三个通道各读一遍。
 *
 * 同一张图用同一个引擎连跑三次结果会一模一样，所以三次必须是三种不同的输入：
 *  A 整图缩到 2400（正常拍摄，求最好那次）
 *  B A 再去饱和 + 拉对比度（对付塑料卡反光和贴膜）
 *  C 只取卡片上半段放大（对付蓝色小标签、姓名那一块）
 *
 * 三个通道是**串行**跑的：并行会让三份大图同时驻留内存，低端机直接 OOM，
 * 而 ML Kit 内部本来就是排队处理，并行换不来时间。
 */
class OcrEngine : Closeable {

    private val recognizer =
        TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())

    suspend fun readThreeChannels(source: Bitmap): List<String> {
        val out = ArrayList<String>(3)
        // A：整图
        out += runChannel(source) { BitmapOps.scaleToMax(source, MAX_SIDE) }
        // B：整图灰度高对比
        out += runChannel(source) {
            val scaled = BitmapOps.scaleToMax(source, MAX_SIDE)
            try {
                BitmapOps.grayscaleHighContrast(scaled)
            } finally {
                if (scaled !== source && !scaled.isRecycled) scaled.recycle()
            }
        }
        // C：上半段放大（卡片上姓名的位置）
        out += runChannel(source) {
            BitmapOps.cropZoom(source, topFraction = 0f, bottomFraction = 0.55f, maxSide = ROI_SIDE)
        }
        return out
    }

    /**
     * 造一张工作图、识别、立刻回收，任何时刻只有一份大图活着。
     * 注意不能回收 [source]——图比 MAX_SIDE 还小的时候 scaleToMax 会原样返回它，
     * 这时候要是回收了，后面的通道就拿到一张死图。
     */
    private suspend fun runChannel(source: Bitmap, make: () -> Bitmap): String {
        val bmp = make()
        return try {
            recognize(bmp)
        } finally {
            if (bmp !== source && !bmp.isRecycled) bmp.recycle()
        }
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

    /** 退出这个团就把识别器关掉，不然每进一次团都留一个 ML Kit 客户端 */
    override fun close() {
        runCatching { recognizer.close() }
    }

    companion object {
        /**
         * 识别前把长边压到 2400。
         * 之前用 1600，实测把号码数字压到二十几个像素高，正好卡在识别边缘；
         * 抬到 2400 后数字高度回到三十多像素，明显更容易读对。
         */
        const val MAX_SIDE = 2400

        /** 通道 C 的裁片长边：裁完再放大，比超分稳，因为只是把本来就在的像素铺开 */
        const val ROI_SIDE = 2000
    }
}

/** 图片预处理小工具，全部返回新 Bitmap，调用方负责回收。 */
object BitmapOps {

    fun scaleToMax(src: Bitmap, maxSide: Int): Bitmap {
        val longer = maxOf(src.width, src.height)
        // 本来就够小就别复制一份，省一份整图内存；调用方回收时会判断是不是原图
        if (longer <= maxSide) return src
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
     * 取图片一段（身份证的姓名在上半、号码在下半），把这块放大到 maxSide。
     * 这比给整张图做超分有效：不做任何"插值猜笔画"，只是把本来就在原图里的像素按比例放大。
     */
    fun cropZoom(src: Bitmap, topFraction: Float, bottomFraction: Float, maxSide: Int): Bitmap {
        val startY = (src.height * topFraction).toInt().coerceIn(0, src.height - 1)
        val endY = (src.height * bottomFraction).toInt().coerceIn(startY + 1, src.height)
        val cropped = Bitmap.createBitmap(src, 0, startY, src.width, endY - startY)
        // 裁片本身就比 maxSide 小时 scaleToMax 会原样返回它，这时不能回收
        val zoomed = scaleToMax(cropped, maxSide)
        if (zoomed !== cropped && !cropped.isRecycled) cropped.recycle()
        return zoomed
    }
}
