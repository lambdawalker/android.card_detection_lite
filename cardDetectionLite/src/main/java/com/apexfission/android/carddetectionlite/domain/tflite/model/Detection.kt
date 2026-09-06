package com.apexfission.android.carddetectionlite.domain.tflite.model

import android.graphics.Bitmap
import com.apexfission.android.carddetectionlite.domain.coordinates.models.ImageBox


data class Detection2(
    val box: ImageBox, val confidence: Float, val classId: Int
)

data class Feature(
    val box: ImageBox, val confidence: Float, val classId: Int, val image: Bitmap
)

