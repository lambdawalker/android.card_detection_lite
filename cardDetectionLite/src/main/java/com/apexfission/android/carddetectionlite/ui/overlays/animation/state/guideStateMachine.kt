package com.apexfission.android.carddetectionlite.ui.overlays.animation.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.apexfission.android.carddetectionlite.domain.coordinates.models.ImageSpaceChain
import com.apexfission.android.carddetectionlite.domain.tflite.model.CardDetection
import com.apexfission.android.carddetectionlite.ui.overlays.animation.DetectionAnimationConfig

@Composable
internal fun rememberGuideStateMachine(
    activeDetection: CardDetection?,
    spaceChain: ImageSpaceChain?,
    config: DetectionAnimationConfig,
    detectionSequence: Long
): InternalGuideState {
    var internalGuideState by remember { mutableStateOf(InternalGuideState.IDLE) }
    val detectionLostTimestamp = remember { mutableLongStateOf(0L) }

    LaunchedEffect(detectionSequence, spaceChain, config) {
        if (detectionSequence == 0L || activeDetection == null || spaceChain == null) {
            if (detectionLostTimestamp.longValue == 0L) {
                detectionLostTimestamp.longValue = System.currentTimeMillis()
            }
            val timeSinceLastDetection = System.currentTimeMillis() - detectionLostTimestamp.longValue
            if (activeDetection == null && timeSinceLastDetection >= config.resetDetectionIndicatorTime) {
                internalGuideState = InternalGuideState.IDLE
            }
        } else {
            detectionLostTimestamp.longValue = 0L
            internalGuideState = if (activeDetection.lockOnProgress >= 1f) {
                InternalGuideState.LOCKED
            } else {
                InternalGuideState.LOCKING
            }
        }
    }

    return internalGuideState
}
