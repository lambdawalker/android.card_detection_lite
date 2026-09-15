package com.apexfission.android.carddetectionlite.ui.overlays.animation.x

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import com.apexfission.android.carddetectionlite.domain.coordinates.models.ImageBox
import com.apexfission.android.carddetectionlite.ui.overlays.animation.DetectionAnimationConfig

@Composable
internal fun rememberAnimatedCoordinates(
    targetBox: ImageBox?,
    fallbackCenterBox: ImageBox,
    boundsDurationMs: Int,
    config: DetectionAnimationConfig,
): ImageBox {
    val activeTargetBox = targetBox ?: fallbackCenterBox

    val trackingSpec = tween<Int>(durationMillis = config.trackingAnimationDurationMs, easing = FastOutSlowInEasing)
    val resetSpec = tween<Int>(durationMillis = config.resetAnimationDurationMs, easing = FastOutSlowInEasing)

    val currentBoundsSpec = when {
        boundsDurationMs == 0 || !config.enableGuideSmoothing -> snap()
        else -> trackingSpec
    }

    val animatedLeft by animateIntAsState(
        targetValue = activeTargetBox.left,
        animationSpec = currentBoundsSpec,
        label = "guideLeft"
    )
    val animatedTop by animateIntAsState(
        targetValue = activeTargetBox.top,
        animationSpec = currentBoundsSpec,
        label = "guideTop"
    )
    val animatedRight by animateIntAsState(
        targetValue = activeTargetBox.right,
        animationSpec = currentBoundsSpec,
        label = "guideRight"
    )
    val animatedBottom by animateIntAsState(
        targetValue = activeTargetBox.bottom,
        animationSpec = currentBoundsSpec,
        label = "guideBottom"
    )

    return ImageBox.from2P(animatedLeft, animatedTop, animatedRight, animatedBottom)
}
