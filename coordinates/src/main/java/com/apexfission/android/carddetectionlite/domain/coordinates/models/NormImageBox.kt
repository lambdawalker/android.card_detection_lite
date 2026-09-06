package com.apexfission.android.carddetectionlite.domain.coordinates.models

/**
 * Represents a 2D bounding box with normalized floating-point coordinates (typically [0.0, 1.0]).
 * Defined by top-left (x, y), bottom-right (x2, y2), and dimensions (width, height).
 *
 * (x, y) is guaranteed to be top-left, and (x2, y2) bottom-right.
 *
 * @property x The normalized top-left X-coordinate.
 * @property y The normalized top-left Y-coordinate.
 * @property x2 The normalized bottom-right X-coordinate.
 * @property y2 The normalized bottom-right Y-coordinate.
 * @property width The normalized width (x2 - x).
 * @property height The normalized height (y2 - y).
 */
data class NormImageBox(
    val x: Float,
    val y: Float,
    val x2: Float,
    val y2: Float,
    val width: Float,
    val height: Float
) {
    companion object {
        /**
         * Constructs a [NormImageBox] from two points: (x1, y1) and (x2, y2).
         * Ensures (x, y) is top-left and (x2, y2) is bottom-right.
         */
        fun from2P(x1: Float, y1: Float, x2: Float, y2: Float): NormImageBox {
            val minX = minOf(x1, x2)
            val maxX = maxOf(x1, x2)
            val minY = minOf(y1, y2)
            val maxY = maxOf(y1, y2)
            return NormImageBox(
                x = minX,
                y = minY,
                x2 = maxX,
                y2 = maxY,
                width = maxX - minX,
                height = maxY - minY
            )
        }

        /**
         * Constructs a [NormImageBox] from top-left point (x, y) and dimensions (width, height).
         */
        fun fromPS(x: Float, y: Float, width: Float, height: Float): NormImageBox {
            val validW = maxOf(0f, width)
            val validH = maxOf(0f, height)
            val x2 = x + validW
            val y2 = y + validH
            return NormImageBox(
                x = x,
                y = y,
                x2 = x2,
                y2 = y2,
                width = validW,
                height = validH
            )
        }

        operator fun invoke(x: Float, y: Float, x2: Float, y2: Float): NormImageBox = from2P(x, y, x2, y2)
    }
}