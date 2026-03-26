package com.apexfission.android.carddetectionlite.domain.tflite.model

import android.graphics.Bitmap
import android.graphics.Rect

data class ExtractedFeature(
    val detection: Detection,
    val coordinates: Rect,
    val objectBitmap: Bitmap,
    val confidence: Float = detection.confidence,
    val classId: Int = detection.classId
)

data class ExtractedFeatures(
    val extractedFeatures: List<ExtractedFeature>,
    val imageWidth: Int,
    val imageHeight: Int
)

fun buildDetection(originalBitmap: Bitmap, detection: Detection, padding: Int = 0): ExtractedFeature {
    val coordinates = detectionToRect(detection, originalBitmap.width, originalBitmap.height, padding)
    val cutoff = Bitmap.createBitmap(originalBitmap, coordinates.left, coordinates.top, coordinates.width(), coordinates.height())
    return ExtractedFeature(detection = detection, coordinates = coordinates, objectBitmap = cutoff)
}

private fun detectionToRect(detection: Detection, originalWidth: Int, originalHeight: Int, padding: Int = 0): Rect {
    val x1 = (detection.x1Pct * originalWidth).toInt() - padding
    val y1 = (detection.y1Pct * originalHeight).toInt() - padding
    val x2 = (detection.x2Pct * originalWidth).toInt() + padding
    val y2 = (detection.y2Pct * originalHeight).toInt() + padding

    val left = x1.coerceIn(0, originalWidth - 1)
    val top = y1.coerceIn(0, originalHeight - 1)
    val right = x2.coerceIn(left + 1, originalWidth)   // ensure width >= 1
    val bottom = y2.coerceIn(top + 1, originalHeight)   // ensure height >= 1

    return Rect(left, top, right, bottom)
}
