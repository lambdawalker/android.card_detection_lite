package com.apexfission.android.carddetectionlite.domain.tflite.model

import android.graphics.Bitmap
import android.graphics.Rect

data class ExtractedFeature(
    val detection: Detection,
    val coordinates: Rect,
    val contextCoordinates: Rect,
    val objectBitmap: Bitmap,
    val confidence: Float = detection.confidence,
    val classId: Int = detection.classId
)

data class ExtractedFeatures(
    val extractedFeatures: List<ExtractedFeature>, val contextWidth: Int, val contextHeight: Int, val originalWidth: Int, val originalHeight: Int
)

fun buildDetection(originalBitmap: Bitmap, detection: Detection, padding: Int = 0): ExtractedFeature {
    val coordinates = percentageCoordinatesToRect(
        detection, originalBitmap.width, originalBitmap.height
    )


    val contextCoordinates = percentageCoordinatesContextToRect(
        detection, originalBitmap.width, originalBitmap.height
    )

    val cutoff = Bitmap.createBitmap(
        originalBitmap, coordinates.left - padding, coordinates.top - padding, coordinates.width() + padding, coordinates.height() + padding
    )

    return ExtractedFeature(
        detection = detection, coordinates = coordinates, contextCoordinates = contextCoordinates, objectBitmap = cutoff
    )
}


private fun percentageCoordinatesToRect(detection: Detection, originalWidth: Int, originalHeight: Int): Rect {
    val x1 = (detection.x1Pct * originalWidth).toInt()
    val y1 = (detection.y1Pct * originalHeight).toInt()
    val x2 = (detection.x2Pct * originalWidth).toInt()
    val y2 = (detection.y2Pct * originalHeight).toInt()

    val left = x1.coerceIn(0, originalWidth - 1)
    val top = y1.coerceIn(0, originalHeight - 1)
    val right = x2.coerceIn(left + 1, originalWidth)   // ensure width >= 1
    val bottom = y2.coerceIn(top + 1, originalHeight)   // ensure height >= 1

    return Rect(left, top, right, bottom)
}

private fun percentageCoordinatesContextToRect(detection: Detection, contextWidth: Int, contextHeight: Int): Rect {
    val x1 = (detection.contextX1Pct * contextWidth).toInt()
    val y1 = (detection.contextY1Pct * contextHeight).toInt()
    val x2 = (detection.contextX2Pct * contextWidth).toInt()
    val y2 = (detection.contextY2Pct * contextHeight).toInt()

    val left = x1.coerceIn(0, contextWidth - 1)
    val top = y1.coerceIn(0, contextHeight - 1)
    val right = x2.coerceIn(left + 1, contextWidth)   // ensure width >= 1
    val bottom = y2.coerceIn(top + 1, contextHeight)   // ensure height >= 1

    return Rect(left, top, right, bottom)
}
