package com.apexfission.android.carddetectionlite.domain.tflite

import android.graphics.Bitmap
import androidx.camera.core.ImageProxy

fun ImageProxy.toUprightBitmap(): Bitmap {
    val bitmap = this.toBitmap()
    val upright = ImageProcessor.rotateIfNeeded(bitmap, this.imageInfo.rotationDegrees)
    return upright
}
