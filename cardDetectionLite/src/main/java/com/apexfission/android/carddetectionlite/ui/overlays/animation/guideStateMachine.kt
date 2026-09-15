package com.apexfission.android.carddetectionlite.ui.overlays.animation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import com.apexfission.android.carddetectionlite.domain.coordinates.models.ImageBox
import com.apexfission.android.carddetectionlite.domain.coordinates.models.ImageSpaceChain
import com.apexfission.android.carddetectionlite.domain.coordinates.transformations.translate
import com.apexfission.android.carddetectionlite.domain.tflite.model.CardDetection
import com.apexfission.android.carddetectionlite.ui.overlays.animation.x.GuideStateResult

@Composable
internal fun rememberGuideStateMachine(
    activeDetection: CardDetection?,
    spaceChain: ImageSpaceChain?,
    fallbackCenterBox: ImageBox,
    config: DetectionAnimationConfig,
    detectionSequence: Long
): GuideStateResult {

    var lastKnownDetectionBox by remember { mutableStateOf(fallbackCenterBox) }
    var targetBox by remember { mutableStateOf(fallbackCenterBox) }

    var targetOpacity by remember {
        mutableFloatStateOf(if (activeDetection != null) config.detectedOpacity else config.idleOpacity)
    }

    val opacityAnimationDuration by remember { mutableIntStateOf(config.fadeAnimationDurationMs) }
    val boundsAnimationDuration by remember { mutableIntStateOf(config.resetAnimationDurationMs) }
    val latestConfig by rememberUpdatedState(config)
    val latestFallbackBox by rememberUpdatedState(fallbackCenterBox)

    val detectionLostTimestamp = remember { mutableLongStateOf(0L) }

    LaunchedEffect(detectionSequence, spaceChain, config, fallbackCenterBox) {

        if (activeDetection != null && spaceChain != null) {
            val translatedBox = activeDetection.card.box.translate(spaceChain)
            lastKnownDetectionBox = translatedBox
        }

        targetBox = lastKnownDetectionBox ?: latestFallbackBox

        if (activeDetection == null) {
            if (detectionLostTimestamp.longValue == 0L) detectionLostTimestamp.longValue = System.currentTimeMillis()
            val timeSinceLastDetection = System.currentTimeMillis() - detectionLostTimestamp.longValue

            if (timeSinceLastDetection >= config.resetBoundingBoxWaitTimeMs) {
                targetBox = latestFallbackBox
            }
        } else {
            detectionLostTimestamp.longValue = 0L
        }

        targetOpacity = if (activeDetection != null) {
            latestConfig.detectedOpacity
        } else {
            latestConfig.idleOpacity
        }
    }

    return GuideStateResult(
        targetBox = targetBox,
        targetOpacity = targetOpacity,
        opacityDurationMs = opacityAnimationDuration,
        boundsDurationMs = boundsAnimationDuration
    )
}
