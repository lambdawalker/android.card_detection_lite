package com.apexfission.android.carddetectionlite.ui.overlays.animation.state

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue

@Composable
internal fun rememberContinuousAnimations(
    isTracking: Boolean,
    opacity: Float,
    motionEnabled: Boolean,
    hasVisibleBounds: Boolean
): Pair<Float, Float> {
    val runContinuousAnimations = shouldRunContinuousAnimations(
        isTracking = isTracking,
        opacity = opacity,
        motionEnabled = motionEnabled,
        hasVisibleBounds = hasVisibleBounds,
    )

    var breathe = 1f
    var sweepPhase = 0f
    if (runContinuousAnimations) {
        val infiniteTransition = rememberInfiniteTransition(label = "detection_bounds_infinite")

        val animatedBreathe by infiniteTransition.animateFloat(
            initialValue = 0.96f,
            targetValue = 1.04f,
            animationSpec = infiniteRepeatable(
                animation = tween(1600, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "breathe"
        )

        val animatedSweepPhase by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(1200, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "sweepPhase"
        )

        breathe = animatedBreathe
        sweepPhase = animatedSweepPhase
    }

    return breathe to sweepPhase
}
