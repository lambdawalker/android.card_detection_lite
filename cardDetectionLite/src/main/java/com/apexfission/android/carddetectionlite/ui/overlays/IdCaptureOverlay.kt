package com.apexfission.android.carddetectionlite.ui.overlays

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.apexfission.android.carddetectionlite.ui.detector.CardDetectorOverlayScope

/**
 * Built-in ID capture overlay following the standard ID verification UI layout.
 *
 * Renders a back button, top text instructions, an animated card guide frame,
 * a bottom-center shutter button, and a flashlight toggle button.
 *
 * Built on top of [AnimatedDetectionCanvas].
 *
 * @param config Configuration parameters for opacity, timers, smoothing, and capture behavior.
 */
@Composable
fun CardDetectorOverlayScope.IdCaptureOverlay(
    config: IdCaptureOverlayConfig = IdCaptureOverlayConfig()
) {
    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedDetectionCanvas(
            config = DetectionAnimationConfig(
                idleOpacity = config.idleOpacity,
                detectedOpacity = config.detectedOpacity,
                missingCardResetDelayMs = config.missingCardResetDelayMs,
                maxConsecutiveMisses = config.maxConsecutiveMisses,
                trackingAnimationDurationMs = config.trackingAnimationDurationMs,
                resetAnimationDurationMs = config.resetAnimationDurationMs,
                fadeAnimationDurationMs = config.fadeAnimationDurationMs,
                enableGuideSmoothing = config.enableGuideSmoothing
            )
        ) {
            val left = bounds.left
            val top = bounds.top
            val right = bounds.right
            val bottom = bounds.bottom

            val color = config.guideColor.copy(alpha = bounds.opacity)

            val cornerLen = 15.dp.toPx()
            val strokeWidth = 3.dp.toPx()

            // Padding to offset the corner brackets from the dashed line
            val cornerPadding = 4.dp.toPx()
            val pLeft = left - cornerPadding
            val pTop = top - cornerPadding
            val pRight = right + cornerPadding
            val pBottom = bottom + cornerPadding

            // Outer dashed boundary line
            val dashedPath = Path().apply {
                addRoundRect(
                    androidx.compose.ui.geometry.RoundRect(
                        left = left, top = top, right = right, bottom = bottom, cornerRadius = CornerRadius(12.dp.toPx(), 10.dp.toPx())
                    )
                )
            }

            drawPath(
                path = dashedPath, color = color, style = Stroke(
                    width = 2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(16f, 12f))
                )
            )

            // Top-Left corner bracket
            drawPath(
                path = Path().apply {
                    moveTo(pLeft, pTop + cornerLen)
                    lineTo(pLeft, pTop + 12.dp.toPx())
                    quadraticBezierTo(pLeft, pTop, pLeft + 12.dp.toPx(), pTop)
                    lineTo(pLeft + cornerLen, pTop)
                }, color = color, style = Stroke(width = strokeWidth)
            )

            // Top-Right corner bracket
            drawPath(
                path = Path().apply {
                    moveTo(pRight - cornerLen, pTop)
                    lineTo(pRight - 12.dp.toPx(), pTop)
                    quadraticBezierTo(pRight, pTop, pRight, pTop + 12.dp.toPx())
                    lineTo(pRight, pTop + cornerLen)
                }, color = color, style = Stroke(width = strokeWidth)
            )

            // Bottom-Right corner bracket
            drawPath(
                path = Path().apply {
                    moveTo(pRight, pBottom - cornerLen)
                    lineTo(pRight, pBottom - 12.dp.toPx())
                    quadraticBezierTo(pRight, pBottom, pRight - 12.dp.toPx(), pBottom)
                    lineTo(pRight - cornerLen, pBottom)
                }, color = color, style = Stroke(width = strokeWidth)
            )

            // Bottom-Left corner bracket
            drawPath(
                path = Path().apply {
                    moveTo(pLeft + cornerLen, pBottom)
                    lineTo(pLeft + 12.dp.toPx(), pBottom)
                    quadraticBezierTo(pLeft, pBottom, pLeft, pBottom - 12.dp.toPx())
                    lineTo(pLeft, pBottom - cornerLen)
                }, color = color, style = Stroke(width = strokeWidth)
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(180.dp)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Black,
                            Color.Black.copy(alpha = 0.75f),
                            Color.Transparent
                        )
                    )
                )
        )

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(180.dp)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.75f),
                            Color.Black
                        )
                    )
                )
        )

        // Overlay UI layer (Header, instructions, controls)
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Bar with back button & title
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = { goBack() }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White
                    )
                }

                Text(
                    text = config.title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center
                )

                // Spacer for top bar balance
                Spacer(modifier = Modifier.size(48.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Instruction title and subtitle
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = config.instructionTitle, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = config.instructionSubTitle, color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp, textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            val isCaptureEnabled = if (config.requiresCardDetectionForCapture) captureEnabled else true

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 32.dp, vertical = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Spacer on left to balance right button
                Spacer(modifier = Modifier.size(48.dp))

                // Centered Shutter / Capture Button
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .alpha(if (isCaptureEnabled) 1.0f else 0.4f)
                        .clickable(
                            enabled = isCaptureEnabled,
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { capture() }), contentAlignment = Alignment.Center) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .border(width = 4.dp, color = Color.White, shape = CircleShape)
                    )
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .background(color = Color.White, shape = CircleShape)
                    )
                }

                if (flashlightAvailable) {
                    IconButton(
                        onClick = { toggleFlashlight() }, modifier = Modifier
                            .size(48.dp)
                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(
                            imageVector = if (flashlightEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff,
                            contentDescription = "Toggle Flashlight",
                            tint = if (flashlightEnabled) Color.Yellow else Color.White
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.size(48.dp))
                }
            }
        }
    }
}
