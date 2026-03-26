package com.apexfission.android.carddetectionlite.domain.tflite.image

import android.graphics.Rect

/**
 * Convert a rect from the original ImageProxy buffer coordinate space to the "upright" bitmap space
 * after applying rotationDegrees.
 *
 * ImageProxy: width/height describe the buffer BEFORE rotation.
 * After 90/270 rotation, upright bitmap size becomes (srcH x srcW).
 */
fun rotateRectToUpright(rect: Rect, srcW: Int, srcH: Int, rotationDegrees: Int): Rect {
    return when ((rotationDegrees % 360 + 360) % 360) {
        0 -> Rect(rect)
        90 -> {
            // uprightW = srcH, uprightH = srcW
            Rect(
                rect.top,
                srcW - rect.right,
                rect.bottom,
                srcW - rect.left
            )
        }

        180 -> {
            Rect(
                srcW - rect.right,
                srcH - rect.bottom,
                srcW - rect.left,
                srcH - rect.top
            )
        }

        270 -> {
            // uprightW = srcH, uprightH = srcW
            Rect(
                srcH - rect.bottom,
                rect.left,
                srcH - rect.top,
                rect.right
            )
        }

        else -> Rect(rect)
    }
}
