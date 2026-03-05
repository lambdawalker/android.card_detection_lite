package com.apexfission.android.carddetectionlite.domain.tflite.data

data class RawDet(val x1Pct: Float, val y1Pct: Float, val x2Pct: Float, val y2Pct: Float, val confidence: Float, val classId: Int)
