package com.apexfission.android.carddetectionlite.domain.tflite.model

import android.graphics.Bitmap
import android.graphics.Rect

data class Detection(
    val rawDetection: RawDetection,
    val coordinates: Rect,
    val objectBitmap: Bitmap,
    val confidence: Float = rawDetection.confidence,
    val classId: Int = rawDetection.classId
)

data class Detections(
    val detections: List<Detection>,
    val imageWidth: Int,
    val imageHeight: Int
)

fun buildDetection(originalBitmap: Bitmap, rawDetection: RawDetection, padding: Int = 0): Detection {
    val r = rawDetectionToPixels(rawDetection, originalBitmap.width, originalBitmap.height, padding)
    val cut = Bitmap.createBitmap(originalBitmap, r.left, r.top, r.width(), r.height())
    return Detection(rawDetection = rawDetection, coordinates = r, objectBitmap = cut)
}

private fun rawDetectionToPixels(rawDetection: RawDetection, originalWidth: Int, originalHeight: Int, padding: Int = 0): Rect {
    val x1 = (rawDetection.x1Pct * originalWidth).toInt() - padding
    val y1 = (rawDetection.y1Pct * originalHeight).toInt() - padding
    val x2 = (rawDetection.x2Pct * originalWidth).toInt() + padding
    val y2 = (rawDetection.y2Pct * originalHeight).toInt() + padding

    val left = x1.coerceIn(0, originalWidth - 1)
    val top = y1.coerceIn(0, originalHeight - 1)
    val right = x2.coerceIn(left + 1, originalWidth)   // ensure width >= 1
    val bottom = y2.coerceIn(top + 1, originalHeight)   // ensure height >= 1

    return Rect(left, top, right, bottom)
}
