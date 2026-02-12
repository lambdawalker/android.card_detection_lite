package com.apexfission.android.carddetectionlite

/**
 * Detection output in crop-normalized coords: [0..1] relative to cropBitmap width/height.
 */
data class Det(
    val x1: Float, val y1: Float, val x2: Float, val y2: Float, val score: Float, val cls: Int = 0
)