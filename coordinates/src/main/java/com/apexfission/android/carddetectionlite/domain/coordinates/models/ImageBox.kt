package com.apexfission.android.carddetectionlite.domain.coordinates.models

/**
 * Represents a 2D bounding box defined by top-left (x, y), bottom-right (x2, y2),
 * and dimensions (width, height).
 *
 * All coordinates and dimensions are unsigned integers in bitmap pixel space.
 * (x, y) is guaranteed to be top-left, and (x2, y2) bottom-right.
 *
 * @property x The top-left X-coordinate.
 * @property y The top-left Y-coordinate.
 * @property x2 The bottom-right X-coordinate.
 * @property y2 The bottom-right Y-coordinate.
 * @property width The horizontal dimension (x2 - x).
 * @property height The vertical dimension (y2 - y).
 */
data class ImageBox(
    val x: UInt,
    val y: UInt,
    val x2: UInt,
    val y2: UInt,
    val width: UInt,
    val height: UInt
) {
    /**
     * Returns a new [ImageBox] shifted by [dx] and [dy].
     */
    fun offset(dx: Int, dy: Int): ImageBox {
        return from2P(
            x1 = (x.toInt() + dx).coerceAtLeast(0),
            y1 = (y.toInt() + dy).coerceAtLeast(0),
            x2 = (x2.toInt() + dx).coerceAtLeast(0),
            y2 = (y2.toInt() + dy).coerceAtLeast(0)
        )
    }

    companion object {
        /**
         * Constructs an [ImageBox] from two points: (x1, y1) and (x2, y2).
         * Ensures (x, y) is top-left and (x2, y2) is bottom-right.
         */
        fun from2P(x1: UInt, y1: UInt, x2: UInt, y2: UInt): ImageBox {
            val minX = minOf(x1, x2)
            val maxX = maxOf(x1, x2)
            val minY = minOf(y1, y2)
            val maxY = maxOf(y1, y2)
            return ImageBox(
                x = minX,
                y = minY,
                x2 = maxX,
                y2 = maxY,
                width = maxX - minX,
                height = maxY - minY
            )
        }

        fun from2P(x1: Int, y1: Int, x2: Int, y2: Int): ImageBox {
            return from2P(
                x1.coerceAtLeast(0).toUInt(),
                y1.coerceAtLeast(0).toUInt(),
                x2.coerceAtLeast(0).toUInt(),
                y2.coerceAtLeast(0).toUInt()
            )
        }

        /**
         * Constructs an [ImageBox] from top-left point (x, y) and dimensions (width, height).
         */
        fun fromPS(x: UInt, y: UInt, width: UInt, height: UInt): ImageBox {
            val x2 = x + width
            val y2 = y + height
            return ImageBox(
                x = x,
                y = y,
                x2 = x2,
                y2 = y2,
                width = width,
                height = height
            )
        }

        fun fromPS(x: Int, y: Int, width: Int, height: Int): ImageBox {
            val validX = x.coerceAtLeast(0).toUInt()
            val validY = y.coerceAtLeast(0).toUInt()
            val validW = width.coerceAtLeast(0).toUInt()
            val validH = height.coerceAtLeast(0).toUInt()
            return fromPS(validX, validY, validW, validH)
        }

        operator fun invoke(x: UInt, y: UInt, x2: UInt, y2: UInt): ImageBox = from2P(x, y, x2, y2)
    }
}