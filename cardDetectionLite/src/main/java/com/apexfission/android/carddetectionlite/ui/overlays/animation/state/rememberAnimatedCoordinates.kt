package com.apexfission.android.carddetectionlite.ui.overlays.animation.state

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.apexfission.android.math.models.ImageBox
import com.apexfission.android.math.models.ImageSpaceChain
import com.apexfission.android.math.transformations.translate
import com.apexfission.android.carddetectionlite.domain.tflite.model.CardDetection
import com.apexfission.android.carddetectionlite.ui.overlays.animation.DetectionAnimationConfig

@Composable
internal fun rememberAnimatedCoordinates(
    activeDetection: CardDetection?,
    spaceChain: ImageSpaceChain?,
    fallbackCenterBox: ImageBox,
    internalGuideState: InternalGuideState,
    boundsDurationMs: Int,
    config: DetectionAnimationConfig,
): ImageBox {
    var lastKnownDetectionBox by remember { mutableStateOf(fallbackCenterBox) }

    if (activeDetection != null && spaceChain != null) {
        lastKnownDetectionBox = activeDetection.card.box.translate(spaceChain)
    }

    val targetBox = when (internalGuideState) {
        InternalGuideState.IDLE -> fallbackCenterBox
        InternalGuideState.LOCKING, InternalGuideState.LOCKED -> lastKnownDetectionBox
    }

    val trackingSpec = tween<Int>(durationMillis = config.trackingAnimationDurationMs, easing = FastOutSlowInEasing)
    val resetSpec = tween<Int>(durationMillis = config.resetAnimationDurationMs, easing = FastOutSlowInEasing)

    val currentBoundsSpec = when {
        boundsDurationMs == 0 || !config.enableGuideSmoothing -> snap()
        internalGuideState == InternalGuideState.IDLE -> resetSpec
        else -> trackingSpec
    }

    val animatedLeft by animateIntAsState(
        targetValue = targetBox.left,
        animationSpec = currentBoundsSpec,
        label = "guideLeft"
    )
    val animatedTop by animateIntAsState(
        targetValue = targetBox.top,
        animationSpec = currentBoundsSpec,
        label = "guideTop"
    )
    val animatedRight by animateIntAsState(
        targetValue = targetBox.right,
        animationSpec = currentBoundsSpec,
        label = "guideRight"
    )
    val animatedBottom by animateIntAsState(
        targetValue = targetBox.bottom,
        animationSpec = currentBoundsSpec,
        label = "guideBottom"
    )

    return ImageBox.from2P(animatedLeft, animatedTop, animatedRight, animatedBottom)
}
