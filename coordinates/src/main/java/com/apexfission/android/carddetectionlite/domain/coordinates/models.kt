package com.apexfission.android.carddetectionlite.domain.coordinates

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
data class ImageSpace(
    val width: UInt,
    val height: UInt,
    val xScale: Float = 1.0F,
    val yScale: Float = 1.0F,
    val xOffset: UInt = 0U,
    val yOffset: UInt = 0U
)

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
 *
 * @param x The X-coordinate in pixels.
 * @param y The Y-coordinate in pixels.
 * @return A new [ImagePoint] instance with [x] and [y] converted to [UInt].
 */
fun ImagePoint(x: Int, y: Int) = ImagePoint(x.toUInt(), y.toUInt())

/**
 * Represents a 2D bounding box defined by two points: top-left (x, y) and bottom-right (x2, y2).
 *
 * @property x The top-left X-coordinate.
 * @property y The top-left Y-coordinate.
 * @property x2 The bottom-right X-coordinate.
 * @property y2 The bottom-right Y-coordinate.
 */
data class ImageBox2P(
    val x: UInt,
    val y: UInt,
    val x2: UInt,
    val y2: UInt
)

/**
 * Represents a 2D bounding box defined by a starting point (x, y) and its width and height.
 *
 * @property x The starting X-coordinate.
 * @property y The starting Y-coordinate.
 * @property width The width of the box.
 * @property height The height of the box.
 */
data class ImageBoxPS(
    val x: UInt,
    val y: UInt,
    val width: Int,
    val height: Int
)

/**
 * Represents a 2D point with normalized floating-point coordinates (typically in the range [0.0, 1.0]).
 *
 * @property x The normalized X-coordinate.
 * @property y The normalized Y-coordinate.
 */
data class NormImagePoint(
    val x: Float,
    val y: Float
)

/**
 * Represents a 2D bounding box with normalized floating-point coordinates defined by two points: (x, y) and (x2, y2).
 *
 * @property x The normalized top-left X-coordinate.
 * @property y The normalized top-left Y-coordinate.
 * @property x2 The normalized bottom-right X-coordinate.
 * @property y2 The normalized bottom-right Y-coordinate.
 */
data class NormImageBox2P(
    val x: Float,
    val y: Float,
    val x2: Float,
    val y2: Float
)

/**
 * Represents a 2D bounding box with normalized floating-point coordinates defined by a starting point (x, y) and dimensions.
 *
 * @property x The normalized starting X-coordinate.
 * @property y The normalized starting Y-coordinate.
 * @property width The normalized width of the box.
 * @property height The normalized height of the box.
 */
data class NormImageBoxPS(
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float
)

typealias ImageSpaceChain = List<ImageSpace>