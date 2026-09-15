package com.apexfission.android.carddetectionlite.ui.overlays.animation.state

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue

@Composable
internal fun rememberAnimatedLockOnProgress(
    internalGuideState: InternalGuideState,
): Pair<Float, Float> {
    val targetProgress = when (internalGuideState) {
        InternalGuideState.IDLE -> 0f
        InternalGuideState.LOCKING -> 0.5f
        InternalGuideState.LOCKED -> 1.0f
    }

    val smoothProgress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = spring(
            stiffness = Spring.StiffnessLow,
            dampingRatio = Spring.DampingRatioNoBouncy
        ),
        label = "smoothProgress"
    )

    return targetProgress to smoothProgress
}
