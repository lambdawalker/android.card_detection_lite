package com.apexfission.android.carddetectionlite.domain.tflite

import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.Rect
import kotlin.math.roundToInt

fun rotateIfNeeded(bm: Bitmap, deg: Int): Bitmap {
    if (deg == 0) return bm
    val m = Matrix().apply { postRotate(deg.toFloat()) }
    return Bitmap.createBitmap(bm, 0, 0, bm.width, bm.height, m, true)
}

fun cropToAspectRatio(src: Bitmap, canvasWidth: Int, canvasHeight: Int, square: Boolean = false): Bitmap {
    val srcWidth = src.width.toFloat()
    val srcHeight = src.height.toFloat()

    val targetAspectRatio = canvasWidth.toFloat() / canvasHeight.toFloat()
    val srcAspectRatio = srcWidth / srcHeight

    var finalCropWidth = srcWidth
    var finalCropHeight = srcHeight

    if (srcAspectRatio > targetAspectRatio) {
        // Image is wider than the canvas needs - crop horizontal sides
        finalCropWidth = srcHeight * targetAspectRatio
    } else if (srcAspectRatio < targetAspectRatio) {
        // Image is taller than the canvas needs - crop vertical top/bottom
        finalCropHeight = srcWidth / targetAspectRatio
    }

    if (square) {
        val size = minOf(finalCropWidth, finalCropHeight)
        finalCropWidth = size
        finalCropHeight = size
    }

    // Calculate offsets for center alignment
    val xOffset = ((srcWidth - finalCropWidth) / 2).roundToInt()
    val yOffset = ((srcHeight - finalCropHeight) / 2).roundToInt()

    return Bitmap.createBitmap(
        src,
        xOffset,
        yOffset,
        finalCropWidth.roundToInt(),
        finalCropHeight.roundToInt()
    )
}

fun centerCropSquare(src: Bitmap, maxSize: Int = Int.MAX_VALUE): Bitmap {
    val size = minOf(src.width, src.height, maxSize)

    val left = (src.width - size) / 2
    val top = (src.height - size) / 2
    val cropRect = Rect(left, top, left + size, top + size)

    return Bitmap.createBitmap(src, cropRect.left, cropRect.top, cropRect.width(), cropRect.height())
}