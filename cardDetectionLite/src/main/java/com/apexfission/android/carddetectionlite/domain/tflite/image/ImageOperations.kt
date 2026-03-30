package com.apexfission.android.carddetectionlite.domain.tflite.image

import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.Rect
import kotlin.math.roundToInt

/**
 * Rotates a [Bitmap] by a specified angle if necessary.
 *
 * If the provided degree is `0`, this function efficiently returns the original bitmap
 * without performing any operations or creating a new object.
 *
 * @param bm The source [Bitmap] to be rotated.
 * @param deg The angle of rotation in degrees (e.g., 90, 180, 270).
 * @return A new, rotated [Bitmap], or the original `bm` instance if `deg` is 0.
 */
fun rotateIfNeeded(bm: Bitmap, deg: Int): Bitmap {
    if (deg == 0) return bm
    val m = Matrix().apply { postRotate(deg.toFloat()) }
    return Bitmap.createBitmap(bm, 0, 0, bm.width, bm.height, m, true)
}

/**
 * Performs a center crop on a source bitmap to match a target aspect ratio.
 *
 * This function is useful for matching the aspect ratio of a camera sensor's output image
 * to the aspect ratio of the UI view it's displayed on. It calculates the largest
 * possible rectangle within the source image that has the target aspect ratio,
 * effectively cropping from the sides (if the image is too wide) or from the top and
 * bottom (if the image is too tall).
 *
 * @param src The source [Bitmap] to be cropped.
 * @param canvasWidth The width of the target area (e.g., a Composable's width).
 * @param canvasHeight The height of the target area (e.g., a Composable's height).
 * @param square If `true`, the resulting bitmap will be further cropped into the largest
 *               possible square from the center of the aspect-ratio-corrected rectangle.
 * @return A new [Bitmap] cropped to the target aspect ratio.
 */
fun cropToAspectRatio(src: Bitmap, canvasWidth: Int, canvasHeight: Int, square: Boolean = false): Bitmap {
    val srcWidth = src.width.toFloat()
    val srcHeight = src.height.toFloat()

    val targetAspectRatio = canvasWidth.toFloat() / canvasHeight.toFloat()
    val srcAspectRatio = srcWidth / srcHeight

    var finalCropWidth = srcWidth
    var finalCropHeight = srcHeight

    if (srcAspectRatio > targetAspectRatio) {
        // Source is wider than target: crop the sides.
        finalCropWidth = srcHeight * targetAspectRatio
    } else if (srcAspectRatio < targetAspectRatio) {
        // Source is taller than target: crop the top and bottom.
        finalCropHeight = srcWidth / targetAspectRatio
    }

    if (square) {
        val size = minOf(finalCropWidth, finalCropHeight)
        finalCropWidth = size
        finalCropHeight = size
    }

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

/**
 * Crops the largest possible square from the center of a source [Bitmap].
 *
 * The side length of the resulting square is determined by the smaller of the source
 * bitmap's width and height. An optional `maxSize` can be provided to limit the
 * output dimensions further.
 *
 * @param src The source [Bitmap].
 * @param maxSize An optional upper limit for the width and height of the resulting square.
 *                The final size will be `min(src.width, src.height, maxSize)`.
 * @return A new, square [Bitmap] cropped from the center of the source.
 */
fun centerCropSquare(src: Bitmap, maxSize: Int = Int.MAX_VALUE): Bitmap {
    val size = minOf(src.width, src.height, maxSize)

    val left = (src.width - size) / 2
    val top = (src.height - size) / 2
    val cropRect = Rect(left, top, left + size, top + size)

    return Bitmap.createBitmap(src, cropRect.left, cropRect.top, cropRect.width(), cropRect.height())
}
