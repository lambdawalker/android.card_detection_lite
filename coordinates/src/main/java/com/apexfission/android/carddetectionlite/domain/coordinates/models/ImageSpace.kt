package com.apexfission.android.carddetectionlite.domain.coordinates.models

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable


/**
 * Represents a 2D coordinate space defined by dimensions, scale factors, and offsets.
 *
 * @property width The width of the space in pixels.
 * @property height The height of the space in pixels.
 * @property xScale The horizontal scale factor relative to the reference space.
 * @property yScale The vertical scale factor relative to the reference space.
 * @property xOffset The horizontal offset (in pixels) relative to the reference space origin.
 * @property yOffset The vertical offset (in pixels) relative to the reference space origin.
 */
@Immutable
@Stable
data class ImageSpace(
    val width: UInt,
    val height: UInt,
    val xScale: Float = 1.0F,
    val yScale: Float = 1.0F,
    val xOffset: UInt = 0U,
    val yOffset: UInt = 0U
)

fun ImageSpace.scale(x: Float, y: Float): ImageSpace =
    ImageSpace(
        width = (width.toFloat() * x).toUInt(),
        height = (height.toFloat() * x).toUInt(),
        xScale = x,
        yScale = y,
        xOffset = xOffset,
        yOffset = yOffset
    )

fun ImageSpace.scale(f: Float): ImageSpace = scale(f, f)

fun ImageSpace.crop(width: UInt, height: UInt, xOffset: UInt, yOffset: UInt): ImageSpace {
    val clampedXOffset = xOffset.coerceAtMost(this.width)
    val clampedYOffset = yOffset.coerceAtMost(this.height)

    val clampedWidth = width.coerceAtMost(this.width - clampedXOffset)
    val clampedHeight = height.coerceAtMost(this.height - clampedYOffset)

    return ImageSpace(
        width = clampedWidth,
        height = clampedHeight,
        xScale = 1f,
        yScale = 1f,
        xOffset = clampedXOffset,
        yOffset = clampedYOffset
    )
}

fun ImageSpace.cropAtCenter(width: UInt, height: UInt): ImageSpace {
    val xOffset = (this.width - width) / 2u
    val yOffset = (this.height - height) / 2u
    return crop(width, height, xOffset, yOffset)
}

typealias ImageSpaceChain = List<ImageSpace>