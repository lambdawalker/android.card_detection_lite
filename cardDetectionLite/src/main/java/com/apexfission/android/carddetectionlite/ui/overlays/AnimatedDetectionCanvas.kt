package com.apexfission.android.carddetectionlite.ui.overlays

import android.graphics.RectF
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.apexfission.android.carddetectionlite.domain.coordinates.transformations.translate
import com.apexfission.android.carddetectionlite.ui.detector.CardDetectorOverlayScope
import kotlinx.coroutines.delay

private enum class InternalGuideState {
    IDLE,
    TRACKING,
    FADING,
    RESETTING
}

/**
 * Scope wrapper providing both Compose [DrawScope] functions and real-time [AnimatedDetectionBounds].
 */
class AnimatedDetectionScope(
    private val drawScope: DrawScope,
    val bounds: AnimatedDetectionBounds
) : DrawScope by drawScope

/**
 * Remembers and calculates real-time animated screen coordinates and tracking state for card overlays.
 *
 * Handles screen coordinate transformation, smooth bounds animations, fading, and reset timers.
 */
@Composable
fun CardDetectorOverlayScope.rememberAnimatedDetectionBounds(
    config: DetectionAnimationConfig = DetectionAnimationConfig()
): AnimatedDetectionBounds {
    val activeDetection = detectionState
    val spaceChain = imageSpaceChain

    var guideState by remember { mutableStateOf(InternalGuideState.IDLE) }
    var detectionToken by remember { mutableLongStateOf(0L) }
    var consecutiveMisses by remember { mutableIntStateOf(0) }

    var lastDetectedBoxScreen by remember { mutableStateOf<RectF?>(null) }
    var targetBox by remember { mutableStateOf<RectF?>(null) }

    var targetOpacity by remember { mutableStateOf(config.idleOpacity) }
    var opacityDurationMs by remember { mutableIntStateOf(config.fadeAnimationDurationMs) }
    var boundsDurationMs by remember { mutableIntStateOf(config.resetAnimationDurationMs) }

    val defaultCenterBox = remember { RectF(0f, 0f, 0f, 0f) }

    LaunchedEffect(activeDetection, spaceChain) {
        if (activeDetection != null && spaceChain != null) {
            val rawBox = activeDetection.card.box.translate(spaceChain)
            val rectInScreen = RectF(
                rawBox.x.toFloat(),
                rawBox.y.toFloat(),
                rawBox.x2.toFloat(),
                rawBox.y2.toFloat()
            )

            lastDetectedBoxScreen = rectInScreen
            targetBox = rectInScreen
            boundsDurationMs = config.trackingAnimationDurationMs

            consecutiveMisses = 0
            detectionToken++
            guideState = InternalGuideState.TRACKING

            targetOpacity = config.detectedOpacity
            opacityDurationMs = config.trackingAnimationDurationMs
        } else {
            consecutiveMisses++

            if (guideState == InternalGuideState.TRACKING || guideState == InternalGuideState.FADING) {
                guideState = InternalGuideState.FADING
                targetOpacity = config.idleOpacity
                opacityDurationMs = config.fadeAnimationDurationMs

                if (lastDetectedBoxScreen != null) {
                    targetBox = lastDetectedBoxScreen
                }

                val maxMisses = config.maxConsecutiveMisses
                if (maxMisses != null && consecutiveMisses >= maxMisses) {
                    guideState = InternalGuideState.RESETTING
                    targetBox = defaultCenterBox
                    boundsDurationMs = config.resetAnimationDurationMs
                }
            }
        }
    }

    LaunchedEffect(guideState, detectionToken) {
        if (guideState == InternalGuideState.FADING) {
            delay(config.missingCardResetDelayMs)
            guideState = InternalGuideState.RESETTING
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

    val trackingSpec = tween<Float>(durationMillis = config.trackingAnimationDurationMs, easing = FastOutSlowInEasing)
    val resetSpec = tween<Float>(durationMillis = config.resetAnimationDurationMs, easing = FastOutSlowInEasing)

    val currentBoundsSpec = if (guideState == InternalGuideState.RESETTING) resetSpec else trackingSpec

    val animatedLeft by animateFloatAsState(
        targetValue = activeTargetBox.left,
        animationSpec = if (config.enableGuideSmoothing) currentBoundsSpec else snap(),
        label = "guideLeft"
    )
    val animatedTop by animateFloatAsState(
        targetValue = activeTargetBox.top,
        animationSpec = if (config.enableGuideSmoothing) currentBoundsSpec else snap(),
        label = "guideTop"
    )
    val animatedRight by animateFloatAsState(
        targetValue = activeTargetBox.right,
        animationSpec = if (config.enableGuideSmoothing) currentBoundsSpec else snap(),
        label = "guideRight"
    )
    val animatedBottom by animateFloatAsState(
        targetValue = activeTargetBox.bottom,
        animationSpec = if (config.enableGuideSmoothing) currentBoundsSpec else snap(),
        label = "guideBottom"
    )

    return AnimatedDetectionBounds(
        left = animatedLeft,
        top = animatedTop,
        right = animatedRight,
        bottom = animatedBottom,
        lockOnProgress = activeDetection?.lockOnProgress ?: 0f,
        opacity = animatedOpacity,
        isTracking = guideState == InternalGuideState.TRACKING,
        activeDetection = activeDetection
    )
}

/**
 * Renders a full-screen [Canvas] providing animated card detection bounds and state in [AnimatedDetectionScope].
 *
 * Allows developers to easily draw custom overlay graphics relative to animated card bounds.
 *
 * Example usage:
 * ```kotlin
 * controlOverlay = {
 *     AnimatedDetectionCanvas {
 *         if (bounds.opacity > 0f) {
 *             drawRect(
 *                 color = Color.Cyan.copy(alpha = bounds.opacity),
 *                 topLeft = bounds.topLeft,
 *                 size = bounds.size,
 *                 style = Stroke(width = 3.dp.toPx())
 *             )
 *         }
 *     }
 * }
 * ```
 */
@Composable
fun CardDetectorOverlayScope.AnimatedDetectionCanvas(
    modifier: Modifier = Modifier.fillMaxSize(),
    config: DetectionAnimationConfig = DetectionAnimationConfig(),
    onDraw: AnimatedDetectionScope.() -> Unit
) {
    BoxWithConstraints(modifier = modifier) {
        val containerWidth = constraints.maxWidth.toFloat()
        val containerHeight = constraints.maxHeight.toFloat()

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

        val rawBounds = rememberAnimatedDetectionBounds(config = config)

        val bounds = if (rawBounds.left == 0f && rawBounds.top == 0f && rawBounds.right == 0f && rawBounds.bottom == 0f) {
            rawBounds.copy(
                left = defaultCenterBox.left,
                top = defaultCenterBox.top,
                right = defaultCenterBox.right,
                bottom = defaultCenterBox.bottom
            )
        } else {
            rawBounds
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            val scope = AnimatedDetectionScope(drawScope = this, bounds = bounds)
            scope.onDraw()
        }
    }
}
