package com.apexfission.android.carddetectionlite.ui.overlays

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.apexfission.android.carddetectionlite.ui.detector.CardDetectorOverlayScope
import com.apexfission.android.carddetectionlite.ui.overlays.animation.AnimatedDetectionCanvas as AnimationAnimatedDetectionCanvas
import com.apexfission.android.carddetectionlite.ui.overlays.animation.AnimatedDetectionScope as AnimationAnimatedDetectionScope
import com.apexfission.android.carddetectionlite.ui.overlays.animation.x.shouldRunContinuousAnimations as animationShouldRunContinuousAnimations

typealias AnimatedDetectionScope = AnimationAnimatedDetectionScope

/**
 * Renders a full-screen Canvas providing animated card detection bounds and state in [AnimatedDetectionScope].
 */
@Composable
fun CardDetectorOverlayScope.AnimatedDetectionCanvas(
    modifier: Modifier = Modifier,
    config: DetectionAnimationConfig = DetectionAnimationConfig(),
    onDraw: AnimatedDetectionScope.() -> Unit
) {
    AnimationAnimatedDetectionCanvas(
        modifier = modifier,
        config = config,
        onDraw = { onDraw() }
    )
}

fun shouldRunContinuousAnimations(
    isTracking: Boolean,
    opacity: Float,
    motionEnabled: Boolean,
    hasVisibleBounds: Boolean
): Boolean = animationShouldRunContinuousAnimations(isTracking, opacity, motionEnabled, hasVisibleBounds)
