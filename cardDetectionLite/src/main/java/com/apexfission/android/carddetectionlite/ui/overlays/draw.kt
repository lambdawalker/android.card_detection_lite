package com.apexfission.android.carddetectionlite.ui.overlays

import android.graphics.BlurMaskFilter
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin


internal data class RoundedSegment(
    val points: List<Pair<Float, Float>>, val rounded: Boolean, val isCorner: Boolean
)

internal fun DrawScope.drawBlurredPath(
    points: List<Pair<Float, Float>>,
    blurRadius: Float,
    color: Color,
    strokeWidth: Float,
    blurStyle: BlurMaskFilter.Blur = BlurMaskFilter.Blur.NORMAL,
    rounded: Boolean = false,
    cornerRadius: Float = 0f
) {
    if (points.size < 2) return

    val path = if (rounded && points.size >= 3) {
        buildRoundedPolylinePath(points, cornerRadius.coerceAtLeast(strokeWidth * 0.75f))
    } else {
        Path().apply {
            val (startX, startY) = points.first()
            moveTo(startX, startY)

            for (i in 1 until points.size) {
                val (x, y) = points[i]
                lineTo(x, y)
            }
        }
    }

    drawIntoCanvas { canvas ->
        val paint = Paint().asFrameworkPaint().apply {
            isAntiAlias = true
            style = android.graphics.Paint.Style.STROKE
            this.strokeWidth = strokeWidth
            this.color = color.toArgb()
            strokeCap = android.graphics.Paint.Cap.ROUND
            strokeJoin = android.graphics.Paint.Join.ROUND

            if (blurRadius > 0f) {
                maskFilter = BlurMaskFilter(blurRadius, blurStyle)
            }
        }

        canvas.nativeCanvas.drawPath(path.asAndroidPath(), paint)
    }
}

internal fun DrawScope.drawGlowPath(
    points: List<Pair<Float, Float>>,
    blurRadius: Float,
    color: Color,
    strokeWidth: Float,
    rounded: Boolean = false,
    cornerRadius: Float = 0f,
) {
    drawBlurredPath(
        points = points, blurRadius = blurRadius, color = color, strokeWidth = strokeWidth, rounded = rounded, cornerRadius = cornerRadius
    )

    drawBlurredPath(
        points = points,
        blurRadius = blurRadius / 2f,
        color = Color.White.copy(alpha = 0.65f),
        strokeWidth = strokeWidth * 0.75f,
        rounded = rounded,
        cornerRadius = cornerRadius
    )

    drawBlurredPath(
        points = points, blurRadius = 0f, color = color, strokeWidth = strokeWidth, rounded = rounded, cornerRadius = cornerRadius
    )
}

fun buildRoundedPolylinePath(
    points: List<Pair<Float, Float>>, radius: Float
): Path {
    val result = Path()
    if (points.size < 2) return result

    val pts = points.map { Offset(it.first, it.second) }
    result.moveTo(pts.first().x, pts.first().y)

    if (pts.size == 2) {
        result.lineTo(pts[1].x, pts[1].y)
        return result
    }

    for (i in 1 until pts.lastIndex) {
        val prev = pts[i - 1]
        val curr = pts[i]
        val next = pts[i + 1]

        val v1x = curr.x - prev.x
        val v1y = curr.y - prev.y
        val v2x = next.x - curr.x
        val v2y = next.y - curr.y

        val len1 = hypot(v1x.toDouble(), v1y.toDouble()).toFloat()
        val len2 = hypot(v2x.toDouble(), v2y.toDouble()).toFloat()

        if (len1 <= 0.001f || len2 <= 0.001f) {
            result.lineTo(curr.x, curr.y)
            continue
        }

        val cut = min(radius, min(len1 / 2f, len2 / 2f))

        val startX = curr.x - (v1x / len1) * cut
        val startY = curr.y - (v1y / len1) * cut
        val endX = curr.x + (v2x / len2) * cut
        val endY = curr.y + (v2y / len2) * cut

        result.lineTo(startX, startY)
        result.quadraticBezierTo(curr.x, curr.y, endX, endY)
    }

    result.lineTo(pts.last().x, pts.last().y)
    return result
}

internal fun lerpF(start: Float, stop: Float, fraction: Float): Float {
    return start + (stop - start) * fraction.coerceIn(0f, 1f)
}


internal fun buildCorner(
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


internal fun buildCorners(
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

internal fun buildConnector(
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


internal fun buildConnectors(
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