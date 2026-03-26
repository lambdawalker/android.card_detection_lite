package com.apexfission.android.carddetectionlite.domain.tflite.model

data class CardDetection(
    val card: ExtractedFeature,
    val features: List<ExtractedFeature>
)