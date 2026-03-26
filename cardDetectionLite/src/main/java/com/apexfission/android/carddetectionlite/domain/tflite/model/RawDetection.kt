package com.apexfission.android.carddetectionlite.domain.tflite.model

data class RawDetection(
    val x1Pct: Float, val y1Pct: Float, val x2Pct: Float, val y2Pct: Float, val confidence: Float, val classId: Int
)

data class RawDetections(
    val rawDetections: List<RawDetection>, val imageWidth: Int, val imageHeight: Int
)