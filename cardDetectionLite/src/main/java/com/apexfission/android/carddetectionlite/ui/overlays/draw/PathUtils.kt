package com.apexfission.android.carddetectionlite.ui.overlays.draw

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
import kotlin.math.hypot
import kotlin.math.min

/**
 * Draws a line segment or rounded path with an optional blur mask filter.
 */
fun DrawScope.drawBlurredPath(
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

/**
 * Draws a path with multiple pass glowing stroke layers.
 */
fun DrawScope.drawGlowPath(
    points: List<Pair<Float, Float>>,
    blurRadius: Float,
    color: Color,
    strokeWidth: Float,
    rounded: Boolean = false,
    cornerRadius: Float = 0f,
) {
    drawBlurredPath(
        points = points,
        blurRadius = blurRadius,
        color = color,
        strokeWidth = strokeWidth,
        rounded = rounded,
        cornerRadius = cornerRadius
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
        points = points,
        blurRadius = 0f,
        color = color,
        strokeWidth = strokeWidth,
        rounded = rounded,
        cornerRadius = cornerRadius
    )
}

/**
 * Builds a Compose [Path] connecting control points with rounded corners.
 */
fun buildRoundedPolylinePath(
    points: List<Pair<Float, Float>>,
    radius: Float
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

/**
 * Linear interpolation helper.
 */
fun lerpF(start: Float, stop: Float, fraction: Float): Float {
    return start + (stop - start) * fraction.coerceIn(0f, 1f)
}
