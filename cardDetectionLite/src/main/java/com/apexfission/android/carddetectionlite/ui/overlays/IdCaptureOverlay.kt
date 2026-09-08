package com.apexfission.android.carddetectionlite.ui.overlays

import android.graphics.RectF
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.apexfission.android.carddetectionlite.domain.coordinates.transformations.translate
import com.apexfission.android.carddetectionlite.ui.detector.CardDetectorOverlayScope
import kotlinx.coroutines.delay

private enum class GuideState {
    IDLE,
    TRACKING,
    FADING,
    RESETTING
}

/**
 * Built-in ID capture overlay following the standard ID verification UI layout.
 *
 * Renders a back button, top text instructions, an animated card guide frame,
 * a bottom-center shutter button, and a flashlight toggle button.
 *
 * @param config Configuration parameters for opacity, timers, smoothing, and capture behavior.
 */
@Composable
fun CardDetectorOverlayScope.IdCaptureOverlay(
    config: IdCaptureOverlayConfig = IdCaptureOverlayConfig()
) {
    val activeDetection = detectionState
    val spaceChain = imageSpaceChain

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val containerWidth = constraints.maxWidth.toFloat()
        val containerHeight = constraints.maxHeight.toFloat()

        // Calculate standard centered ID card guide box (aspect ratio ~1.586)
        val defaultCenterBox = remember(containerWidth, containerHeight) {
            val aspectRatio = 1.58577f
            var width = containerWidth * 0.85f
            var height = width / aspectRatio
            if (height > containerHeight * 0.55f) {
                height = containerHeight * 0.55f
                width = height * aspectRatio
            }
            val left = (containerWidth - width) / 2f
            val top = (containerHeight - height) / 2.2f
            RectF(left, top, left + width, top + height)
        }

        var guideState by remember { mutableStateOf(GuideState.IDLE) }
        var detectionToken by remember { mutableLongStateOf(0L) }
        var consecutiveMisses by remember { mutableIntStateOf(0) }

        var lastDetectedBoxScreen by remember { mutableStateOf<RectF?>(null) }
        var targetBox by remember { mutableStateOf<RectF?>(null) }

        var targetOpacity by remember { mutableStateOf(config.idleOpacity) }
        var opacityDurationMs by remember { mutableIntStateOf(config.fadeAnimationDurationMs) }
        var boundsDurationMs by remember { mutableIntStateOf(config.resetAnimationDurationMs) }

        // Detection update state machine
        LaunchedEffect(activeDetection, spaceChain) {
            if (activeDetection != null && spaceChain != null) {
                val rawBox = activeDetection.card.box.translate(spaceChain)
                val rectInScreen = RectF(
                    rawBox.x.toFloat(),
                    rawBox.y.toFloat(),
                    rawBox.x2.toFloat(),
                    rawBox.y2.toFloat()
                )

                val smoothedRect = if (config.enableGuideSmoothing && lastDetectedBoxScreen != null) {
                    val factor = config.guideSmoothingFactor.coerceIn(0.01f, 1f)
                    val prev = lastDetectedBoxScreen!!
                    RectF(
                        prev.left + (rectInScreen.left - prev.left) * factor,
                        prev.top + (rectInScreen.top - prev.top) * factor,
                        prev.right + (rectInScreen.right - prev.right) * factor,
                        prev.bottom + (rectInScreen.bottom - prev.bottom) * factor
                    )
                } else {
                    rectInScreen
                }

                lastDetectedBoxScreen = smoothedRect
                targetBox = smoothedRect
                boundsDurationMs = config.trackingAnimationDurationMs

                consecutiveMisses = 0
                detectionToken++
                guideState = GuideState.TRACKING

                targetOpacity = config.detectedOpacity
                opacityDurationMs = config.trackingAnimationDurationMs
            } else {
                consecutiveMisses++

                if (guideState == GuideState.TRACKING || guideState == GuideState.FADING) {
                    guideState = GuideState.FADING
                    targetOpacity = config.idleOpacity
                    opacityDurationMs = config.fadeAnimationDurationMs

                    if (lastDetectedBoxScreen != null) {
                        targetBox = lastDetectedBoxScreen
                    }

                    val maxMisses = config.maxConsecutiveMisses
                    if (maxMisses != null && consecutiveMisses >= maxMisses) {
                        guideState = GuideState.RESETTING
                        targetBox = defaultCenterBox
                        boundsDurationMs = config.resetAnimationDurationMs
                    }
                }
            }
        }

        // Delay timer for missing card reset
        LaunchedEffect(guideState, detectionToken) {
            if (guideState == GuideState.FADING) {
                delay(config.missingCardResetDelayMs)
                guideState = GuideState.RESETTING
                targetBox = defaultCenterBox
                boundsDurationMs = config.resetAnimationDurationMs
                targetOpacity = config.idleOpacity
            }
        }

        val animatedOpacity by animateFloatAsState(
            targetValue = targetOpacity,
            animationSpec = tween(durationMillis = opacityDurationMs, easing = FastOutSlowInEasing),
            label = "guideOpacity"
        )

        val activeTargetBox = targetBox ?: defaultCenterBox

        val animatedLeft by animateFloatAsState(
            targetValue = activeTargetBox.left,
            animationSpec = if (config.enableGuideSmoothing) tween(durationMillis = boundsDurationMs, easing = FastOutSlowInEasing) else snap(),
            label = "guideLeft"
        )
        val animatedTop by animateFloatAsState(
            targetValue = activeTargetBox.top,
            animationSpec = if (config.enableGuideSmoothing) tween(durationMillis = boundsDurationMs, easing = FastOutSlowInEasing) else snap(),
            label = "guideTop"
        )
        val animatedRight by animateFloatAsState(
            targetValue = activeTargetBox.right,
            animationSpec = if (config.enableGuideSmoothing) tween(durationMillis = boundsDurationMs, easing = FastOutSlowInEasing) else snap(),
            label = "guideRight"
        )
        val animatedBottom by animateFloatAsState(
            targetValue = activeTargetBox.bottom,
            animationSpec = if (config.enableGuideSmoothing) tween(durationMillis = boundsDurationMs, easing = FastOutSlowInEasing) else snap(),
            label = "guideBottom"
        )

        // Render card guide canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val left = animatedLeft
            val top = animatedTop
            val right = animatedRight
            val bottom = animatedBottom

            val width = (right - left).coerceAtLeast(1f)
            val height = (bottom - top).coerceAtLeast(1f)

            val color = config.guideColor.copy(alpha = animatedOpacity)

            val cornerLen = 28.dp.toPx()
            val strokeWidth = 3.dp.toPx()

            // Outer dashed boundary line
            val dashedPath = Path().apply {
                addRoundRect(
                    androidx.compose.ui.geometry.RoundRect(
                        left = left,
                        top = top,
                        right = right,
                        bottom = bottom,
                        cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx())
                    )
                )
            }
            drawPath(
                path = dashedPath,
                color = color,
                style = Stroke(
                    width = 2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(16f, 12f))
                )
            )

            // Top-Left corner bracket
            drawPath(
                path = Path().apply {
                    moveTo(left, top + cornerLen)
                    lineTo(left, top + 12.dp.toPx())
                    quadraticBezierTo(left, top, left + 12.dp.toPx(), top)
                    lineTo(left + cornerLen, top)
                },
                color = color,
                style = Stroke(width = strokeWidth)
            )

            // Top-Right corner bracket
            drawPath(
                path = Path().apply {
                    moveTo(right - cornerLen, top)
                    lineTo(right - 12.dp.toPx(), top)
                    quadraticBezierTo(right, top, right, top + 12.dp.toPx())
                    lineTo(right, top + cornerLen)
                },
                color = color,
                style = Stroke(width = strokeWidth)
            )

            // Bottom-Right corner bracket
            drawPath(
                path = Path().apply {
                    moveTo(right, bottom - cornerLen)
                    lineTo(right, bottom - 12.dp.toPx())
                    quadraticBezierTo(right, bottom, right - 12.dp.toPx(), bottom)
                    lineTo(right - cornerLen, bottom)
                },
                color = color,
                style = Stroke(width = strokeWidth)
            )

            // Bottom-Left corner bracket
            drawPath(
                path = Path().apply {
                    moveTo(left + cornerLen, bottom)
                    lineTo(left + 12.dp.toPx(), bottom)
                    quadraticBezierTo(left, bottom, left, bottom - 12.dp.toPx())
                    lineTo(left, bottom - cornerLen)
                },
                color = color,
                style = Stroke(width = strokeWidth)
            )
        }

        // Overlay UI layer (Header, instructions, controls)
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Bar with back button & title
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = { goBack() }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }

                Text(
                    text = config.title,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )

                // Spacer for top bar balance
                Spacer(modifier = Modifier.size(48.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Instruction title and subtitle
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = config.instructionTitle,
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = config.instructionSubTitle,
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Bottom controls section: Shutter and Flashlight
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
                            onClick = { capture() }
                        ),
                    contentAlignment = Alignment.Center
                ) {
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

                // Flashlight toggle button on bottom right (replaces camera-flip button)
                if (flashlightAvailable) {
                    IconButton(
                        onClick = { toggleFlashlight() },
                        modifier = Modifier
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
