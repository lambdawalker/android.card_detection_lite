package com.apexfission.android.carddetectionlite.ui.overlays

import android.graphics.BlurMaskFilter
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import com.apexfission.android.carddetectionlite.domain.coordinates.models.ImageSpaceChain
import com.apexfission.android.carddetectionlite.domain.coordinates.transformations.translate
import com.apexfission.android.carddetectionlite.domain.tflite.model.CardDetection
import kotlin.math.hypot
import kotlin.math.min

import com.apexfission.android.carddetectionlite.ui.detector.CardDetectorOverlayScope

/**
 * A stylized, animated overlay providing visual feedback during the card lock-on process.
 *
 * Renders a glowing, animated frame around the card whose progress and styling change dynamically
 * based on `activeDetection.lockOnProgress`. Coordinates are mapped to screen space using [imageSpaceChain].
 */
@Composable
fun CardDetectorOverlayScope.CardLockOnOverlay() {
    val spaceChain = imageSpaceChain ?: return
    CardLockOnOverlay(
        activeDetection = detectionState,
        imageSpaceChain = spaceChain
    )
}

/**
 * Standalone variant of [CardLockOnOverlay] taking explicit parameters.
 *
 * @param activeDetection The current [CardDetection] from the ViewModel.
 * @param imageSpaceChain The [ImageSpaceChain] used to map detection coordinates to screen space.
 */
@Composable
fun CardLockOnOverlay(
    activeDetection: CardDetection?, imageSpaceChain: ImageSpaceChain
) {
    val card = activeDetection?.card ?: return
    val box = card.box.translate(imageSpaceChain)
    val lockOnProgress = activeDetection.lockOnProgress

    val tweenSpec = tween<Float>(durationMillis = 200, easing = FastOutSlowInEasing)

    val progressSpring = spring<Float>(
        stiffness = Spring.StiffnessLow, dampingRatio = Spring.DampingRatioNoBouncy
    )

    val smoothX1 by animateFloatAsState(box.x.toFloat(), tweenSpec, label = "x1")
    val smoothY1 by animateFloatAsState(box.y.toFloat(), tweenSpec, label = "y1")
    val smoothX2 by animateFloatAsState(box.x2.toFloat(), tweenSpec, label = "x2")
    val smoothY2 by animateFloatAsState(box.y2.toFloat(), tweenSpec, label = "y2")
    val smoothProgress by animateFloatAsState(
        targetValue = lockOnProgress.coerceIn(0f, 1f), animationSpec = progressSpring, label = "progress"
    )

    val infinite = rememberInfiniteTransition(label = "lock_on_overlay")

    val breathe by infinite.animateFloat(
        initialValue = 0.96f, targetValue = 1.04f, animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse
        ), label = "breathe"
    )

    val sweepPhase by infinite.animateFloat(
        initialValue = 0f, targetValue = 1f, animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing), repeatMode = RepeatMode.Restart
        ), label = "sweep"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val idlePadding = 28.dp.toPx()
        val lockedPadding = 4.dp.toPx()
        val currentPadding = lerpF(idlePadding, lockedPadding, smoothProgress)

        val left = smoothX1 - currentPadding
        val top = smoothY1 - currentPadding
        val right = smoothX2 + currentPadding
        val bottom = smoothY2 + currentPadding

        val frameWidth = (right - left).coerceAtLeast(1f)
        val frameHeight = (bottom - top).coerceAtLeast(1f)
        if (frameWidth < 2f || frameHeight < 2f) return@Canvas

        val radius = 22.dp.toPx()
        val strokeWidth = 0.8.dp.toPx()

        val idleColor = Color.White.copy(alpha = 0.42f)
        val trackingColor = Color(0xFF9BE7FF).copy(alpha = 0.95f)
        val lockedColor = Color(0xFF4CFF98)

        val frameColor = when {
            smoothProgress < 0.55f -> lerp(idleColor, trackingColor, smoothProgress / 0.55f)
            else -> lerp(trackingColor, lockedColor, (smoothProgress - 0.55f) / 0.45f)
        }

        val glowColor = frameColor.copy(alpha = lerpF(0.45f, 0.85f, smoothProgress))
        val pulse = if (smoothProgress < 0.15f) breathe else 1f
        val blurRadius = lerpF(6.dp.toPx(), 12.dp.toPx(), smoothProgress) * pulse

        val cornerLen = lerpF(18.dp.toPx(), 24.dp.toPx(), smoothProgress)
        val tickLen = lerpF(7.dp.toPx(), 9.dp.toPx(), smoothProgress)

        val midY = (top + bottom) / 2f
        val inset = radius * 0.45f

        val frameSegments = listOf(
            RoundedSegment(
                points = listOf(
                    left to (top + inset + cornerLen), left to (top + inset), (left + inset) to top, (left + inset + cornerLen) to top
                ), rounded = true, isCorner = true
            ),
            RoundedSegment(
                points = listOf(
                    (left + inset + cornerLen) to top + strokeWidth, (right - inset - cornerLen) to top + strokeWidth
                ), rounded = false, isCorner = false
            ),
            RoundedSegment(
                points = listOf(
                    (left + inset + cornerLen) to top - strokeWidth, (right - inset - cornerLen) to top - strokeWidth
                ), rounded = false, isCorner = false
            ),
            RoundedSegment(
                points = listOf(
                    (right - inset - cornerLen) to top, (right - inset) to top, right to (top + inset), right to (top + inset + cornerLen)
                ), rounded = true, isCorner = true
            ),
            RoundedSegment(
                points = listOf(
                    right to (top + inset + cornerLen), right to (midY - tickLen)
                ), rounded = false, isCorner = false
            ),
            RoundedSegment(
                points = listOf(
                    right to (midY + tickLen), right to (bottom - inset - cornerLen)
                ), rounded = false, isCorner = false
            ),
            RoundedSegment(
                points = listOf(
                    right to (bottom - inset - cornerLen), right to (bottom - inset), (right - inset) to bottom, (right - inset - cornerLen) to bottom
                ), rounded = true, isCorner = true
            ),
            RoundedSegment(
                points = listOf(
                    (right - inset - cornerLen) to bottom, (left + inset + cornerLen) to bottom
                ), rounded = false, isCorner = false
            ),
            RoundedSegment(
                points = listOf(
                    (left + inset + cornerLen) to bottom, (left + inset) to bottom, left to (bottom - inset), left to (bottom - inset - cornerLen)
                ), rounded = true, isCorner = true
            ),
            RoundedSegment(
                points = listOf(
                    left to (bottom - inset - cornerLen), left to (midY + tickLen)
                ), rounded = false, isCorner = false
            ),
            RoundedSegment(
                points = listOf(
                    left to (midY - tickLen), left to (top + inset + cornerLen)
                ), rounded = false, isCorner = false
            )
        )

        frameSegments.forEach { segment ->
            val segmentStroke = if (segment.isCorner) strokeWidth * 6f else strokeWidth
            val cornerRadiusPx = min(segmentStroke * 1.6f, 10.dp.toPx())

            drawGlowPath(
                points = segment.points,
                blurRadius = blurRadius,
                color = glowColor,
                strokeWidth = segmentStroke,
                rounded = segment.rounded,
                cornerRadius = cornerRadiusPx
            )
        }

        if (smoothProgress > 0.72f) {
            val sweepAlpha = ((smoothProgress - 0.72f) / 0.28f).coerceIn(0f, 1f)
            val sweepWidth = frameWidth * 0.005f
            val sweepX = left + (frameWidth + sweepWidth) * sweepPhase - sweepWidth
            val startX = sweepX.coerceAtLeast(left + inset + cornerLen)
            val endX = (sweepX + sweepWidth).coerceAtMost(right - inset - cornerLen)

            if (endX > startX) {
                drawGlowPath(
                    points = listOf(startX to top, endX to top),
                    blurRadius = blurRadius,
                    color = Color.White.copy(alpha = 0.85f * sweepAlpha),
                    strokeWidth = strokeWidth * 2f
                )

                drawGlowPath(
                    points = listOf(startX to top, endX to top),
                    blurRadius = blurRadius,
                    color = Color.White.copy(alpha = 0.85f * sweepAlpha),
                    strokeWidth = strokeWidth * 2f
                )

                drawGlowPath(
                    points = listOf(startX to top, endX to top),
                    blurRadius = blurRadius,
                    color = Color.White.copy(alpha = 0.85f * sweepAlpha),
                    strokeWidth = strokeWidth * 4f
                )
            }
        }
    }
}

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

private fun buildRoundedPolylinePath(
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
