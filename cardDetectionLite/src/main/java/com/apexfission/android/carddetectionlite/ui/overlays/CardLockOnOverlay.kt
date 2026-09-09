package com.apexfission.android.carddetectionlite.ui.overlays

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import com.apexfission.android.carddetectionlite.ui.detector.CardDetectorOverlayScope
import com.apexfission.android.carddetectionlite.ui.overlays.draw.buildConnectors
import com.apexfission.android.carddetectionlite.ui.overlays.draw.buildCorners
import com.apexfission.android.carddetectionlite.ui.overlays.draw.drawGlowPath
import com.apexfission.android.carddetectionlite.ui.overlays.draw.lerpF
import kotlin.math.min

/**
 * Lock-on overlay using AnimatedDetectionCanvas.
 *
 * Progress, breathe, and sweepPhase are automatically animated and exposed by AnimatedDetectionCanvas.
 * When detection disappears, the last progress is retained while the overlay fades out.
 */
@Composable
fun CardDetectorOverlayScope.CardLockOnOverlay(
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

        // Animate frame stroke thickness when an ID is detected/locked on
        val idleConnectorStroke = 1.2.dp.toPx()
        val lockedConnectorStroke = 4.5.dp.toPx()
        val connectorStroke = lerpF(idleConnectorStroke, lockedConnectorStroke, progress)

        val idleCornerStroke = 5.0.dp.toPx()
        val lockedCornerStroke = 13.0.dp.toPx()
        val cornerStroke = lerpF(idleCornerStroke, lockedCornerStroke, progress)

        // Brighter electric blue color scheme for ID detection & lock-on
        val idleColor = Color.White.copy(alpha = 0.45f * bounds.opacity)
        val trackingColor = Color(0xFF40C4FF).copy(alpha = 0.95f * bounds.opacity) // Bright Electric Blue
        val lockedColor = Color(0xFF00E5FF).copy(alpha = bounds.opacity)           // Brighter Neon Blue/Cyan

        val frameColor = when {
            progress < 0.50f ->
                lerp(idleColor, trackingColor, progress / 0.50f)

            else ->
                lerp(trackingColor, lockedColor, (progress - 0.50f) / 0.50f)
        }

        val glowColor = frameColor.copy(
            alpha = lerpF(
                0.50f * bounds.opacity,
                0.95f * bounds.opacity,
                progress
            )
        )

        val pulse = if (progress < 0.15f) breathe else 1f
        val blurRadius = lerpF(8.dp.toPx(), 20.dp.toPx(), progress) * pulse
        val cornerLen = lerpF(20.dp.toPx(), 32.dp.toPx(), progress)

        val inset = radius * 0.45f

        val frameSegments =
            buildCorners(
                left = left,
                top = top,
                right = right,
                bottom = bottom,
                cornerLength = cornerLen,
                cornerRadius = cornerLen / 2
            ) + buildConnectors(
                left = left,
                top = top,
                right = right,
                bottom = bottom,
                cornerLength = cornerLen,
                gap = connectorStroke
            )

        frameSegments.forEach { segment ->
            val segmentStroke = if (segment.isCorner) cornerStroke else connectorStroke
            val cornerRadius = min(segmentStroke * 1.6f, 12.dp.toPx())

            drawGlowPath(
                points = segment.points,
                blurRadius = blurRadius,
                color = glowColor,
                strokeWidth = segmentStroke,
                rounded = segment.rounded,
                cornerRadius = cornerRadius
            )
        }

        if (progress > 0.65f) {
            val sweepAlpha =
                ((progress - 0.65f) / 0.35f).coerceIn(0f, 1f) * bounds.opacity

            val sweepWidth = frameWidth * 0.008f
            val sweepX =
                left + (frameWidth + sweepWidth) * sweepPhase - sweepWidth

            val startX = sweepX.coerceAtLeast(left + inset + cornerLen)
            val endX = (sweepX + sweepWidth).coerceAtMost(
                right - inset - cornerLen
            )

            if (endX > startX) {
                val sweepPoints = listOf(startX to top, endX to top)
                val sweepColor = Color(0xFFE0F7FA).copy(alpha = 0.9f * sweepAlpha)

                listOf(2, 3, 5).forEach { multiplier ->
                    drawGlowPath(
                        points = sweepPoints,
                        blurRadius = blurRadius,
                        color = sweepColor,
                        strokeWidth = connectorStroke * multiplier
                    )
                }
            }
        }
    }
}
