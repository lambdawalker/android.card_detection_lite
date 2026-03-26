package com.apexfission.android.carddetectionlite.domain.tflite.model

import android.graphics.Bitmap
import android.graphics.Rect

data class Detection(
    val rawDet: RawDet,
    val coordinates: Rect,
    val objectBitmap: Bitmap,
    val confidence: Float = rawDet.confidence,
    val classId: Int = rawDet.classId
)

fun buildDetection(originalBitmap: Bitmap, rawDet: RawDet, padding: Int = 0): Detection {
    val r = rawDetectionToPixels(rawDet, originalBitmap.width, originalBitmap.height, padding)
    val cut = Bitmap.createBitmap(originalBitmap, r.left, r.top, r.width(), r.height())
    return Detection(rawDet = rawDet, coordinates = r, objectBitmap = cut)
}

private fun rawDetectionToPixels(rawDet: RawDet, originalWidth: Int, originalHeight: Int, padding: Int = 0): Rect {
    val x1 = (rawDet.x1Pct * originalWidth).toInt() - padding
    val y1 = (rawDet.y1Pct * originalHeight).toInt() - padding
    val x2 = (rawDet.x2Pct * originalWidth).toInt() + padding
    val y2 = (rawDet.y2Pct * originalHeight).toInt() + padding

    val left = x1.coerceIn(0, originalWidth - 1)
    val top = y1.coerceIn(0, originalHeight - 1)
    val right = x2.coerceIn(left + 1, originalWidth)   // ensure width >= 1
    val bottom = y2.coerceIn(top + 1, originalHeight)   // ensure height >= 1

    return Rect(left, top, right, bottom)
}
