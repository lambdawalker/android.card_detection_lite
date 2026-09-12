package com.apexfission.android.carddetectionlite.ui.overlays

import android.graphics.RectF
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
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
import com.apexfission.android.carddetectionlite.ui.detector.ConsecutiveMissTracker
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
 * Handles screen coordinate transformation, smooth bounds animations, fading, lock-on spring progress,
 * breathing pulse, sweep phase, and reset timers.
 *
 * @param defaultBox Optional default bounding box to use when no card is actively detected (e.g. centered guide frame).
 * @param config Animation threshold and timing configuration.
 */
@Composable
fun CardDetectorOverlayScope.rememberAnimatedDetectionBounds(
    defaultBox: RectF? = null,
    config: DetectionAnimationConfig = DetectionAnimationConfig()
): AnimatedDetectionBounds {
    val activeDetection = detectionState
    val spaceChain = imageSpaceChain

    val fallbackCenterBox = remember(defaultBox) { defaultBox ?: RectF(0f, 0f, 0f, 0f) }

    val initialTargetBox = remember(activeDetection, spaceChain, fallbackCenterBox) {
        if (activeDetection != null && spaceChain != null) {
            val rawBox = activeDetection.card.box.translate(spaceChain)
            RectF(
                rawBox.x.toFloat(),
                rawBox.y.toFloat(),
                rawBox.x2.toFloat(),
                rawBox.y2.toFloat()
            )
        } else {
            if (fallbackCenterBox.width() > 0f && fallbackCenterBox.height() > 0f) fallbackCenterBox else null
        }
    }

    var guideState by remember {
        mutableStateOf(
            if (activeDetection != null && spaceChain != null) InternalGuideState.TRACKING else InternalGuideState.IDLE
        )
    }
    var detectionToken by remember { mutableLongStateOf(0L) }
    val consecutiveMissTracker = remember { ConsecutiveMissTracker() }
    var isFirstDetection by remember { mutableStateOf(true) }

    var lastDetectedBoxScreen by remember { mutableStateOf<RectF?>(initialTargetBox) }
    var targetBox by remember { mutableStateOf<RectF?>(initialTargetBox) }

    var targetOpacity by remember {
        mutableStateOf(if (activeDetection != null) config.detectedOpacity else config.idleOpacity)
    }
    var opacityDurationMs by remember { mutableIntStateOf(config.fadeAnimationDurationMs) }
    var boundsDurationMs by remember { mutableIntStateOf(config.resetAnimationDurationMs) }

    LaunchedEffect(detectionSequence, spaceChain) {
        if (detectionSequence == 0L) {
            consecutiveMissTracker.reset()
            guideState = InternalGuideState.IDLE
            targetBox = fallbackCenterBox
            targetOpacity = config.idleOpacity
            return@LaunchedEffect
        }

        val consecutiveMisses = consecutiveMissTracker.record(
            sequence = detectionSequence,
            detected = activeDetection != null,
        )

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

            if (isFirstDetection && (defaultBox == null || defaultBox.isEmpty)) {
                boundsDurationMs = 0
            } else {
                boundsDurationMs = config.trackingAnimationDurationMs
            }

            isFirstDetection = false
            detectionToken++
            guideState = InternalGuideState.TRACKING

            targetOpacity = config.detectedOpacity
            opacityDurationMs = config.trackingAnimationDurationMs
        } else if (activeDetection == null) {
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
                    targetBox = if (config.resetPositionOnMissing) fallbackCenterBox else (lastDetectedBoxScreen ?: fallbackCenterBox)
                    boundsDurationMs = config.resetAnimationDurationMs
                }
            }
        }
    }

    LaunchedEffect(guideState, detectionToken) {
        if (guideState == InternalGuideState.FADING) {
            delay(config.missingCardResetDelayMs)
            guideState = InternalGuideState.RESETTING
            targetBox = if (config.resetPositionOnMissing) fallbackCenterBox else (lastDetectedBoxScreen ?: fallbackCenterBox)
            boundsDurationMs = config.resetAnimationDurationMs
            targetOpacity = config.idleOpacity
        }
    }

    val animatedOpacity by animateFloatAsState(
        targetValue = targetOpacity,
        animationSpec = tween(durationMillis = opacityDurationMs, easing = FastOutSlowInEasing),
        label = "guideOpacity"
    )

    val activeTargetBox = targetBox ?: fallbackCenterBox

    val trackingSpec = tween<Float>(durationMillis = config.trackingAnimationDurationMs, easing = FastOutSlowInEasing)
    val resetSpec = tween<Float>(durationMillis = config.resetAnimationDurationMs, easing = FastOutSlowInEasing)

    val currentBoundsSpec = when {
        boundsDurationMs == 0 || !config.enableGuideSmoothing -> snap()
        guideState == InternalGuideState.RESETTING -> resetSpec
        else -> trackingSpec
    }

    val animatedLeft by animateFloatAsState(
        targetValue = activeTargetBox.left,
        animationSpec = currentBoundsSpec,
        label = "guideLeft"
    )
    val animatedTop by animateFloatAsState(
        targetValue = activeTargetBox.top,
        animationSpec = currentBoundsSpec,
        label = "guideTop"
    )
    val animatedRight by animateFloatAsState(
        targetValue = activeTargetBox.right,
        animationSpec = currentBoundsSpec,
        label = "guideRight"
    )
    val animatedBottom by animateFloatAsState(
        targetValue = activeTargetBox.bottom,
        animationSpec = currentBoundsSpec,
        label = "guideBottom"
    )

    // Smooth spring lock-on progress
    val rawProgress = activeDetection?.lockOnProgress?.coerceIn(0f, 1f) ?: 0f
    val smoothProgress by animateFloatAsState(
        targetValue = rawProgress,
        animationSpec = spring(
            stiffness = Spring.StiffnessLow,
            dampingRatio = Spring.DampingRatioNoBouncy
        ),
        label = "smoothProgress"
    )

    // Continuous breathing pulse
    val infiniteTransition = rememberInfiniteTransition(label = "detection_bounds_infinite")

    val breathe by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathe"
    )

    // Continuous sweep phase
    val sweepPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sweepPhase"
    )

    return AnimatedDetectionBounds(
        left = animatedLeft,
        top = animatedTop,
        right = animatedRight,
        bottom = animatedBottom,
        lockOnProgress = rawProgress,
        smoothProgress = smoothProgress,
        breathe = breathe,
        sweepPhase = sweepPhase,
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

        val bounds = rememberAnimatedDetectionBounds(
            defaultBox = defaultCenterBox,
            config = config
        )

        Canvas(modifier = Modifier.fillMaxSize()) {
            val scope = AnimatedDetectionScope(drawScope = this, bounds = bounds)
            scope.onDraw()
        }
    }
}
