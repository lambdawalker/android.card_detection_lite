package com.apexfission.android.carddetectionlite.domain.tflite.model

import android.graphics.Rect

data class CardDetection(
    val lockOnProgress: Float,
    val id: Int,
    val card: ExtractedFeature,
    val features: List<ExtractedFeature>,
    val contextSize: Rect
)