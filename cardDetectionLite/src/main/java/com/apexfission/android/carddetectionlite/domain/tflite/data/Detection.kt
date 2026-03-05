package com.apexfission.android.carddetectionlite.domain.tflite.data

import android.graphics.Bitmap
import android.graphics.Rect
import com.apexfission.android.carddetectionlite.domain.tflite.rawDetectionToPixels

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