package com.apexfission.android.carddetectionlite.ui.overlays.animation.x

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue

@Composable
fun rememberAnimatedOpacity(
    targetOpacity: Float,
    durationMs: Int
): Float {
    val animatedOpacity by animateFloatAsState(
        targetValue = targetOpacity,
        animationSpec = tween(durationMillis = durationMs, easing = FastOutSlowInEasing),
        label = "guideOpacity"
    )
    return animatedOpacity
}