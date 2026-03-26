package com.apexfission.android.carddetectionlite.domain.tflite.data

import android.graphics.Bitmap

sealed interface PreprocessingResult {
    val bitmap: Bitmap
}

data class LetterboxResult(
    override val bitmap: Bitmap,
    val scale: Float,
    val padX: Float,
    val padY: Float
) : PreprocessingResult
