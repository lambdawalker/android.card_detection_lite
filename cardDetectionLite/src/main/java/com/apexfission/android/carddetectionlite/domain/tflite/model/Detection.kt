package com.apexfission.android.carddetectionlite.domain.tflite.model

data class Detection(
    val x1Pct: Float, val y1Pct: Float, val x2Pct: Float, val y2Pct: Float, val confidence: Float, val classId: Int
)

data class Detections(
    val detections: List<Detection>,
    val contextWidth: Int,
    val contextHeight: Int,
    val originalWidth: Int,
    val originalHeight: Int
)