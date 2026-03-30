package com.apexfission.android.carddetectionlite.domain.tflite.image

import android.graphics.Bitmap
import androidx.camera.core.ImageProxy

/**
 * Converts a CameraX [ImageProxy] into a correctly oriented [Bitmap].
 *
 * This extension function simplifies a common two-step process in camera applications:
 * 1.  It first calls the standard `imageProxy.toBitmap()` method to convert the raw
 *     image buffer (often in a YUV format) into an initial `Bitmap`.
 * 2.  It then reads the rotation metadata from `imageProxy.imageInfo.rotationDegrees`
 *     and applies the necessary rotation to produce a final, "upright" bitmap that
 *     matches the user's perspective.
 *
 * This is crucial for ensuring that image processing and model inference are performed
 * on an image that has the correct orientation.
 *
 * @receiver The [ImageProxy] instance, typically from a CameraX analysis use case.
 * @return A new [Bitmap] instance that is properly rotated to be upright.
 */
fun ImageProxy.toUprightBitmap(): Bitmap {
    val bitmap = this.toBitmap()
    return rotateIfNeeded(bitmap, this.imageInfo.rotationDegrees)
}
