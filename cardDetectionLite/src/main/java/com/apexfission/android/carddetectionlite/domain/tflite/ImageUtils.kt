package com.apexfission.android.carddetectionlite.domain.tflite

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import androidx.camera.core.ImageProxy
import com.apexfission.android.carddetectionlite.domain.tflite.data.Det
import com.apexfission.android.carddetectionlite.domain.tflite.data.DetCutout
import java.io.ByteArrayOutputStream

/* --------------------------- YUV -> RGB --------------------------- */
/**
 * NOTE: This still uses YUV -> JPEG -> Bitmap which works but is slow.
 * Biggest future win: replace this with a real YUV_420_888 -> RGB converter (no JPEG).
 */
class YuvToRgbConverter {
    private val jpegOut = ByteArrayOutputStream(1024 * 256)

    fun toBitmap(image: ImageProxy): Bitmap {
        val nv21 = imageToNv21(image)
        val yuvImage = YuvImage(nv21, ImageFormat.NV21, image.width, image.height, null)

        jpegOut.reset()
        yuvImage.compressToJpeg(Rect(0, 0, image.width, image.height), 90, jpegOut)
        val bytes = jpegOut.toByteArray()
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }

    private fun imageToNv21(image: ImageProxy): ByteArray {
        val yBuffer = image.planes[0].buffer
        val uBuffer = image.planes[1].buffer
        val vBuffer = image.planes[2].buffer

        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()

        val nv21 = ByteArray(ySize + uSize + vSize)
        yBuffer.get(nv21, 0, ySize)

        val chromaRowStride = image.planes[1].rowStride
        val chromaPixelStride = image.planes[1].pixelStride

        val u = ByteArray(uSize).also { uBuffer.get(it) }
        val v = ByteArray(vSize).also { vBuffer.get(it) }

        var offset = ySize
        val width = image.width
        val height = image.height
        val uvHeight = height / 2
        val uvWidth = width / 2

        for (row in 0 until uvHeight) {
            for (col in 0 until uvWidth) {
                val idx = row * chromaRowStride + col * chromaPixelStride
                nv21[offset++] = v[idx]
                nv21[offset++] = u[idx]
            }
        }

        return nv21
    }
}

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

fun Rect.intersectedWith(maxW: Int, maxH: Int): Rect {
    val l = left.coerceIn(0, maxW)
    val t = top.coerceIn(0, maxH)
    val r = right.coerceIn(0, maxW)
    val b = bottom.coerceIn(0, maxH)
    return Rect(l, t, r, b)
}


fun detToRectPx(det: Det, cropW: Int, cropH: Int, padPx: Int = 0): Rect {
    val x1 = (det.x1Pct * cropW).toInt() - padPx
    val y1 = (det.y1Pct * cropH).toInt() - padPx
    val x2 = (det.x2Pct * cropW).toInt() + padPx
    val y2 = (det.y2Pct * cropH).toInt() + padPx

    val left = x1.coerceIn(0, cropW - 1)
    val top = y1.coerceIn(0, cropH - 1)
    val right = x2.coerceIn(left + 1, cropW)   // ensure width >= 1
    val bottom = y2.coerceIn(top + 1, cropH)   // ensure height >= 1

    return Rect(left, top, right, bottom)
}

fun cropDet(cropBitmap: Bitmap, det: Det, padPx: Int = 0): DetCutout {
    val r: Rect = detToRectPx(det, cropBitmap.width, cropBitmap.height, padPx)
    val cut = Bitmap.createBitmap(cropBitmap, r.left, r.top, r.width(), r.height())
    return DetCutout(det = det, rectPx = r, objectBitmap = cut)
}

