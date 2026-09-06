package com.apexfission.android.carddetectionlite.ui

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import com.apexfission.android.carddetectionlite.domain.coordinates.models.ImageSpaceChain
import com.apexfission.android.carddetectionlite.domain.coordinates.transformations.toChildSpace
import com.apexfission.android.carddetectionlite.domain.tflite.model.CardDetection
import com.apexfission.android.carddetectionlite.domain.tflite.model.CardDetection2
import kotlin.math.min

/**
 * A highly stylized and animated overlay that provides visual feedback for the card detection "lock-on" process.
 *
 * This Composable draws a futuristic, glowing frame around the detected card. The frame's appearance
 * and animations change dynamically based on the `lockOnProgress` of the detection, creating a rich
 * user experience that communicates the state of the detection process.
 *
 * @param activeDetection The current [CardDetection] from the ViewModel. The overlay uses this object's
 *                        `lockOnProgress` to drive its animations and its card's coordinates to position
 *                        the frame. If this is `null`, the overlay will not be drawn.
 * @param scalingInfo The [PreviewScalingInfo] necessary to map the detection's normalized coordinates
 *                    to the absolute pixel coordinates of the screen.
 */
@Composable
fun CardLockOnOverlay2(
    activeDetection: CardDetection2?, imageSpaceChain: ImageSpaceChain
) {
    val card = activeDetection?.card ?: return
    val box = card.box.toChildSpace(imageSpaceChain)
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
        3.dp.toPx()
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

