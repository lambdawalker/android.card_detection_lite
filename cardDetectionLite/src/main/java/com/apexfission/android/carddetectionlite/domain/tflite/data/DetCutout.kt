package com.apexfission.android.carddetectionlite.domain.tflite.data

import android.graphics.Bitmap
import android.graphics.Rect

data class DetCutout(
    val rawDet: RawDet,
    val rectPx: Rect,
    val objectBitmap: Bitmap
)