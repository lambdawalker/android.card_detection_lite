package com.apexfission.android.carddetectionlite.domain.tflite.model

import android.graphics.Bitmap

data class LetterboxResult(
    val bitmap: Bitmap,
    val scale: Float,
    val padX: Float,
    val padY: Float
)
