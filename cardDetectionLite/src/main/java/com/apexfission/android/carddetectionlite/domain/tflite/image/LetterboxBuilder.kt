package com.apexfission.android.carddetectionlite.domain.tflite.image

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import androidx.core.graphics.createBitmap
import com.apexfission.android.carddetectionlite.domain.tflite.model.LetterboxResult
import kotlin.math.min

object LetterboxBuilder {

    private var lbOut: Bitmap? = null
    private var lbCanvas: Canvas? = null
    private val lbPaint = Paint(Paint.FILTER_BITMAP_FLAG)
    private val black = Color.BLACK

    fun build(src: Bitmap, targetSize: Int): LetterboxResult {
        val out = lbOut?.takeIf { it.width == targetSize } ?: createBitmap(targetSize, targetSize).also { lbOut = it }

        val canvas = lbCanvas ?: Canvas(out).also { lbCanvas = it }
        val scale = min(targetSize / src.width.toFloat(), targetSize / src.height.toFloat())

        val newW = (src.width * scale).toInt()
        val newH = (src.height * scale).toInt()

        val padX = (targetSize - newW) / 2f
        val padY = (targetSize - newH) / 2f

        canvas.drawColor(black)
        canvas.drawBitmap(src, null, RectF(padX, padY, padX + newW, padY + newH), lbPaint)

        return LetterboxResult(out, scale, padX, padY)
    }

    fun cleanUp() {
        lbOut = null
        lbCanvas = null
    }
}