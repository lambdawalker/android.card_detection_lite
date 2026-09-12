package com.apexfission.android.carddetectionlite.domain.coordinates.models

/**
 * Represents a 2D point with unsigned integer pixel coordinates.
 *
 * @property x The X-coordinate in pixels.
 * @property y The Y-coordinate in pixels.
 */
data class ImagePoint(
    val x: UInt,
    val y: UInt
)

/**
 * Convenience factory function to construct an [ImagePoint] using signed integers [x] and [y].
 * Negative inputs are clamped to zero before conversion.
 *
 * @param x The X-coordinate in pixels.
 * @param y The Y-coordinate in pixels.
 * @return A new [ImagePoint] instance with clamped [x] and [y] converted to [UInt].
 */
fun ImagePoint(x: Int, y: Int) = ImagePoint(x.coerceAtLeast(0).toUInt(), y.coerceAtLeast(0).toUInt())