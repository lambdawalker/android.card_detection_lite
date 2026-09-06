package com.apexfission.android.carddetectionlite.domain.coordinates.models

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