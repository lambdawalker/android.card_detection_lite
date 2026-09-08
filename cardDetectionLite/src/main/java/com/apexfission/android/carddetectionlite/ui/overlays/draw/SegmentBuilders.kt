package com.apexfission.android.carddetectionlite.ui.overlays.draw

import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Builds a corner [RoundedSegment] for a given corner ID (1=top-left, 2=top-right, 3=bottom-right, 4=bottom-left).
 */
fun buildCorner(
    cornerPoint: Pair<Float, Float>,
    cornerLength: Float,
    cornerRadius: Float,
    cornerId: Int
): RoundedSegment {
    require(cornerId in 1..4)

    val (cx, cy) = cornerPoint

    val dx = if ((cornerId / 2) % 2 == 0) 1f else -1f
    val dy = if (((cornerId - 1) / 2) % 2 == 0) 1f else -1f

    val points = listOf(
        cx to (cy + dy * (cornerRadius + cornerLength)),
        cx to (cy + dy * cornerRadius),
        (cx + dx * cornerRadius) to cy,
        (cx + dx * (cornerRadius + cornerLength)) to cy
    )

    return RoundedSegment(
        points = points,
        rounded = true,
        isCorner = true
    )
}

/**
 * Builds corner segments for all 4 corners of a rectangle.
 */
fun buildCorners(
    left: Float,
    top: Float,
    right: Float,
    bottom: Float,
    cornerLength: Float,
    cornerRadius: Float
): List<RoundedSegment> {
    return (1..4).map { cornerId ->
        val dx = if ((cornerId / 2) % 2 == 0) 1f else -1f
        val dy = if (((cornerId - 1) / 2) % 2 == 0) 1f else -1f

        val cornerPoint = Pair(
            if (dx > 0f) left else right,
            if (dy > 0f) top else bottom
        )

        buildCorner(
            cornerPoint = cornerPoint,
            cornerLength = cornerLength,
            cornerRadius = cornerRadius,
            cornerId = cornerId
        )
    }
}

/**
 * Builds straight connector line segments between corners.
 */
fun buildConnector(
    startCorner: Pair<Float, Float>,
    endCorner: Pair<Float, Float>,
    cornerLength: Float,
    gap: Float,
    sideId: Int
): List<RoundedSegment> {
    require(sideId in 0..3)

    val angle = sideId * (Math.PI / 2.0)

    val h = cos(angle).roundToInt().toFloat()
    val v = sin(angle).roundToInt().toFloat()

    val (startX, startY) = startCorner
    val (endX, endY) = endCorner

    val x1 = startX + cornerLength * h
    val y1 = startY + cornerLength * v

    val x2 = endX - cornerLength * h
    val y2 = endY - cornerLength * v

    return listOf(
        RoundedSegment(
            points = listOf(
                (x1 + gap * v) to (y1 + gap * h),
                (x2 + gap * v) to (y2 + gap * h)
            ),
            rounded = false,
            isCorner = false
        ),
        RoundedSegment(
            points = listOf(
                (x1 - gap * v) to (y1 - gap * h),
                (x2 - gap * v) to (y2 - gap * h)
            ),
            rounded = false,
            isCorner = false
        )
    )
}

/**
 * Builds straight connector line segments for all 4 sides of a rectangle.
 */
fun buildConnectors(
    left: Float,
    top: Float,
    right: Float,
    bottom: Float,
    cornerLength: Float,
    gap: Float
): List<RoundedSegment> {
    val corners = listOf(
        left to top,       // top-left
        right to top,      // top-right
        right to bottom,   // bottom-right
        left to bottom,    // bottom-left
        left to top        // close the loop
    )

    return corners
        .zipWithNext()
        .flatMapIndexed { sideId, (start, end) ->
            buildConnector(
                startCorner = start,
                endCorner = end,
                cornerLength = cornerLength,
                gap = gap,
                sideId = sideId
            )
        }
}
