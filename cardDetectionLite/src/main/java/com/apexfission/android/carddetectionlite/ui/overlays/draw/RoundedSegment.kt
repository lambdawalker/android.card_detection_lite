package com.apexfission.android.carddetectionlite.ui.overlays.draw

/**
 * Represents a line segment or rounded corner curve specified by control points.
 */
data class RoundedSegment(
    val points: List<Pair<Float, Float>>,
    val rounded: Boolean,
    val isCorner: Boolean
)
