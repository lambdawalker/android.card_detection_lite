package com.apexfission.android.carddetectionlite.domain.tflite

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import androidx.camera.core.ImageProxy
import com.apexfission.android.carddetectionlite.domain.tflite.data.RawDet
import com.apexfission.android.carddetectionlite.domain.tflite.data.DetCutout
import java.io.ByteArrayOutputStream

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


fun detToRectPx(rawDet: RawDet, cropW: Int, cropH: Int, padPx: Int = 0): Rect {
    val x1 = (rawDet.x1Pct * cropW).toInt() - padPx
    val y1 = (rawDet.y1Pct * cropH).toInt() - padPx
    val x2 = (rawDet.x2Pct * cropW).toInt() + padPx
    val y2 = (rawDet.y2Pct * cropH).toInt() + padPx

    val left = x1.coerceIn(0, cropW - 1)
    val top = y1.coerceIn(0, cropH - 1)
    val right = x2.coerceIn(left + 1, cropW)   // ensure width >= 1
    val bottom = y2.coerceIn(top + 1, cropH)   // ensure height >= 1

    return Rect(left, top, right, bottom)
}

fun cropDet(cropBitmap: Bitmap, rawDet: RawDet, padPx: Int = 0): DetCutout {
    val r: Rect = detToRectPx(rawDet, cropBitmap.width, cropBitmap.height, padPx)
    val cut = Bitmap.createBitmap(cropBitmap, r.left, r.top, r.width(), r.height())
    return DetCutout(rawDet = rawDet, rectPx = r, objectBitmap = cut)
}

