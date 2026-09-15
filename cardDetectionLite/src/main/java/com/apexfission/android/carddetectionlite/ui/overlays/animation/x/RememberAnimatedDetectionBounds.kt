package com.apexfission.android.carddetectionlite.ui.overlays.animation.x

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.apexfission.android.carddetectionlite.domain.coordinates.models.ImageBox
import com.apexfission.android.carddetectionlite.ui.detector.CardDetectorOverlayScope
import com.apexfission.android.carddetectionlite.ui.overlays.animation.AnimatedDetectionBounds
import com.apexfission.android.carddetectionlite.ui.overlays.animation.AnimationEffectsState
import com.apexfission.android.carddetectionlite.ui.overlays.animation.DetectionAnimationConfig
import com.apexfission.android.carddetectionlite.ui.overlays.animation.LockOnProgressState
import com.apexfission.android.carddetectionlite.ui.overlays.animation.TrackingMetadata
import com.apexfission.android.carddetectionlite.ui.overlays.animation.rememberGuideStateMachine

/**
 * Remembers and calculates real-time animated screen coordinates and tracking state for card overlays.
 */
@Composable
fun CardDetectorOverlayScope.rememberAnimatedDetectionBounds(
    defaultBox: ImageBox? = null,
    config: DetectionAnimationConfig = DetectionAnimationConfig()
): AnimatedDetectionBounds {
    val activeDetection = detectionState
    val spaceChain = imageSpaceChain
    val fallbackCenterBox = remember(defaultBox) { defaultBox ?: ImageBox.from2P(0, 0, 0, 0) }

    val guideStateResult = rememberGuideStateMachine(
        activeDetection = activeDetection,
        spaceChain = spaceChain,
        fallbackCenterBox = fallbackCenterBox,
        config = config,
        detectionSequence = detectionSequence
    )

    val animatedCoordinates = rememberAnimatedCoordinates(
        targetBox = guideStateResult.targetBox,
        fallbackCenterBox = fallbackCenterBox,
        boundsDurationMs = guideStateResult.boundsDurationMs,
        config = config
    )

    val animatedOpacity = rememberAnimatedOpacity(
        targetOpacity = guideStateResult.targetOpacity,
        durationMs = guideStateResult.opacityDurationMs
    )

    val (rawProgress, smoothProgress) = rememberAnimatedLockOnProgress(
        activeDetection = activeDetection,
    )

    val (breathe, sweepPhase) = rememberContinuousAnimations(
        isTracking = true,
        opacity = animatedOpacity,
        motionEnabled = config.enableContinuousAnimations,
        hasVisibleBounds = animatedCoordinates.right > animatedCoordinates.left &&
            animatedCoordinates.bottom > animatedCoordinates.top
    )

    return AnimatedDetectionBounds(
        coordinates = animatedCoordinates,
        progress = LockOnProgressState(lockOnProgress = rawProgress, smoothProgress = smoothProgress),
        effects = AnimationEffectsState(breathe = breathe, sweepPhase = sweepPhase, opacity = animatedOpacity),
        tracking = TrackingMetadata(isTracking = true, activeDetection = activeDetection)
    )
}
