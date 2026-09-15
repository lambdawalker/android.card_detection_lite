package com.apexfission.android.carddetectionlite.ui.overlays.animation.x

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.apexfission.android.carddetectionlite.domain.tflite.model.CardDetection

@Composable
internal fun rememberAnimatedLockOnProgress(
    activeDetection: CardDetection?,
): Pair<Float, Float> {
    var lastLockOnProgress by remember { mutableFloatStateOf(activeDetection?.lockOnProgress?.coerceIn(0f, 1f) ?: 0f) }

    val rawProgress = if (activeDetection != null) {
        val prog = activeDetection.lockOnProgress.coerceIn(0f, 1f)
        lastLockOnProgress = prog
        prog
    } else {
        lastLockOnProgress
    }

    val smoothProgress by animateFloatAsState(
        targetValue = rawProgress,
        animationSpec = spring(
            stiffness = Spring.StiffnessLow,
            dampingRatio = Spring.DampingRatioNoBouncy
        ),
        label = "smoothProgress"
    )

    return rawProgress to smoothProgress
}