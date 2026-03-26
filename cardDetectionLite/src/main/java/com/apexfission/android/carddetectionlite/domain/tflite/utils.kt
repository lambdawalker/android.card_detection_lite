package com.apexfission.android.carddetectionlite.domain.tflite

import android.graphics.Rect
import com.apexfission.android.carddetectionlite.domain.tflite.data.RawDet

/* -------------------------- RECT HELPERS -------------------------- */

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


fun rawDetectionToPixels(rawDet: RawDet, originalWidth: Int, originalHeight: Int, padding: Int = 0): Rect {
    val x1 = (rawDet.x1Pct * originalWidth).toInt() - padding
    val y1 = (rawDet.y1Pct * originalHeight).toInt() - padding
    val x2 = (rawDet.x2Pct * originalWidth).toInt() + padding
    val y2 = (rawDet.y2Pct * originalHeight).toInt() + padding

    val left = x1.coerceIn(0, originalWidth - 1)
    val top = y1.coerceIn(0, originalHeight - 1)
    val right = x2.coerceIn(left + 1, originalWidth)   // ensure width >= 1
    val bottom = y2.coerceIn(top + 1, originalHeight)   // ensure height >= 1

    return Rect(left, top, right, bottom)
}

