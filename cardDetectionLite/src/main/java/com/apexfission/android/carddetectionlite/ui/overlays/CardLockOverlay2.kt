package com.apexfission.android.carddetectionlite.ui.overlays

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import com.apexfission.android.carddetectionlite.ui.detector.CardDetectorOverlayScope
import kotlin.math.min

/**
 * Lock-on overlay using AnimatedDetectionCanvas.
 *
 * Progress, breathe, and sweepPhase are automatically animated and exposed by AnimatedDetectionCanvas.
 * When detection disappears, the last progress is retained while the overlay fades out.
 */
@Composable
fun CardDetectorOverlayScope.CardLockOnOverlay2(
    config: DetectionAnimationConfig = DetectionAnimationConfig(
        idleOpacity = 0f,
        detectedOpacity = 1f,
        resetPositionOnMissing = false,
        fadeAnimationDurationMs = 300
    )
) {
    AnimatedDetectionCanvas(config = config) {
        if (bounds.opacity <= 0f) return@AnimatedDetectionCanvas

        val progress = bounds.smoothProgress
        val breathe = bounds.breathe
        val sweepPhase = bounds.sweepPhase

        val idlePadding = 28.dp.toPx()
        val lockedPadding = 4.dp.toPx()
        val padding = lerpF(idlePadding, lockedPadding, progress)

        val left = bounds.left - padding
        val top = bounds.top - padding
        val right = bounds.right + padding
        val bottom = bounds.bottom + padding

        val frameWidth = (right - left).coerceAtLeast(1f)
        val frameHeight = (bottom - top).coerceAtLeast(1f)
        if (frameWidth < 2f || frameHeight < 2f) return@AnimatedDetectionCanvas

        val radius = 22.dp.toPx()
        val strokeWidth = 0.8.dp.toPx()

        val idleColor = Color.White.copy(alpha = 0.42f * bounds.opacity)
        val trackingColor = Color(0xFF9BE7FF).copy(alpha = 0.95f * bounds.opacity)
        val lockedColor = Color(0xFF4CFF98).copy(alpha = bounds.opacity)

        val frameColor = when {
            progress < 0.55f ->
                lerp(idleColor, trackingColor, progress / 0.55f)

            else ->
                lerp(trackingColor, lockedColor, (progress - 0.55f) / 0.45f)
        }

        val glowColor = frameColor.copy(
            alpha = lerpF(
                0.45f * bounds.opacity,
                0.85f * bounds.opacity,
                progress
            )
        )

        val pulse = if (progress < 0.15f) breathe else 1f
        val blurRadius = lerpF(6.dp.toPx(), 12.dp.toPx(), progress) * pulse
        val cornerLen = lerpF(18.dp.toPx(), 24.dp.toPx(), progress)
        val tickLen = lerpF(7.dp.toPx(), 9.dp.toPx(), progress)

        val midY = (top + bottom) / 2f
        val inset = radius * 0.45f

        val frameSegments = listOf(
            RoundedSegment(
                points = listOf(
                    left to (top + inset + cornerLen),
                    left to (top + inset),
                    (left + inset) to top,
                    (left + inset + cornerLen) to top
                ),
                rounded = true,
                isCorner = true
            ),
            RoundedSegment(
                points = listOf(
                    (left + inset + cornerLen) to top + strokeWidth,
                    (right - inset - cornerLen) to top + strokeWidth
                ),
                rounded = false,
                isCorner = false
            ),
            RoundedSegment(
                points = listOf(
                    (left + inset + cornerLen) to top - strokeWidth,
                    (right - inset - cornerLen) to top - strokeWidth
                ),
                rounded = false,
                isCorner = false
            ),
            RoundedSegment(
                points = listOf(
                    (right - inset - cornerLen) to top,
                    (right - inset) to top,
                    right to (top + inset),
                    right to (top + inset + cornerLen)
                ),
                rounded = true,
                isCorner = true
            ),
            RoundedSegment(
                points = listOf(
                    right to (top + inset + cornerLen),
                    right to (midY - tickLen)
                ),
                rounded = false,
                isCorner = false
            ),
            RoundedSegment(
                points = listOf(
                    right to (midY + tickLen),
                    right to (bottom - inset - cornerLen)
                ),
                rounded = false,
                isCorner = false
            ),
            RoundedSegment(
                points = listOf(
                    right to (bottom - inset - cornerLen),
                    right to (bottom - inset),
                    (right - inset) to bottom,
                    (right - inset - cornerLen) to bottom
                ),
                rounded = true,
                isCorner = true
            ),
            RoundedSegment(
                points = listOf(
                    (right - inset - cornerLen) to bottom,
                    (left + inset + cornerLen) to bottom
                ),
                rounded = false,
                isCorner = false
            ),
            RoundedSegment(
                points = listOf(
                    (left + inset + cornerLen) to bottom,
                    (left + inset) to bottom,
                    left to (bottom - inset),
                    left to (bottom - inset - cornerLen)
                ),
                rounded = true,
                isCorner = true
            ),
            RoundedSegment(
                points = listOf(
                    left to (bottom - inset - cornerLen),
                    left to (midY + tickLen)
                ),
                rounded = false,
                isCorner = false
            ),
            RoundedSegment(
                points = listOf(
                    left to (midY - tickLen),
                    left to (top + inset + cornerLen)
                ),
                rounded = false,
                isCorner = false
            )
        )

        frameSegments.forEach { segment ->
            val segmentStroke = if (segment.isCorner) strokeWidth * 6f else strokeWidth
            val cornerRadius = min(segmentStroke * 1.6f, 10.dp.toPx())

            drawGlowPath(
                points = segment.points,
                blurRadius = blurRadius,
                color = glowColor,
                strokeWidth = segmentStroke,
                rounded = segment.rounded,
                cornerRadius = cornerRadius
            )
        }

        if (progress > 0.72f) {
            val sweepAlpha =
                ((progress - 0.72f) / 0.28f).coerceIn(0f, 1f) * bounds.opacity

            val sweepWidth = frameWidth * 0.005f
            val sweepX =
                left + (frameWidth + sweepWidth) * sweepPhase - sweepWidth

            val startX = sweepX.coerceAtLeast(left + inset + cornerLen)
            val endX = (sweepX + sweepWidth).coerceAtMost(
                right - inset - cornerLen
            )

            if (endX > startX) {
                val sweepPoints = listOf(startX to top, endX to top)
                val sweepColor = Color.White.copy(alpha = 0.85f * sweepAlpha)

                drawGlowPath(
                    points = sweepPoints,
                    blurRadius = blurRadius,
                    color = sweepColor,
                    strokeWidth = strokeWidth * 2f
                )

                drawGlowPath(
                    points = sweepPoints,
                    blurRadius = blurRadius,
                    color = sweepColor,
                    strokeWidth = strokeWidth * 2f
                )

                drawGlowPath(
                    points = sweepPoints,
                    blurRadius = blurRadius,
                    color = sweepColor,
                    strokeWidth = strokeWidth * 4f
                )
            }
        }
    }
}
