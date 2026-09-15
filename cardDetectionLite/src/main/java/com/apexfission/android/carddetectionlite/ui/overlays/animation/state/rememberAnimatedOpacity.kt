package com.apexfission.android.carddetectionlite.ui.overlays.animation.state

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import com.apexfission.android.carddetectionlite.ui.overlays.animation.DetectionAnimationConfig

@Composable
fun rememberAnimatedOpacity(
    internalGuideState: InternalGuideState,
    config: DetectionAnimationConfig
): Float {
    val targetOpacity = when (internalGuideState) {
        InternalGuideState.IDLE -> config.idleOpacity
        InternalGuideState.LOCKING, InternalGuideState.LOCKED -> config.detectedOpacity
    }

    val durationMs = when (internalGuideState) {
        InternalGuideState.IDLE -> config.fadeAnimationDurationMs
        InternalGuideState.LOCKING, InternalGuideState.LOCKED -> config.trackingAnimationDurationMs
    }

    val animatedOpacity by animateFloatAsState(
        targetValue = targetOpacity,
        animationSpec = tween(durationMillis = durationMs, easing = FastOutSlowInEasing),
        label = "guideOpacity"
    )
    return animatedOpacity
}
