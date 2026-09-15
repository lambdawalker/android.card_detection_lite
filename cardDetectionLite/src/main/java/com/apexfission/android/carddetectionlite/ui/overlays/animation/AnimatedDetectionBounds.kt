package com.apexfission.android.carddetectionlite.ui.overlays.animation

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import com.apexfission.android.carddetectionlite.domain.coordinates.models.ImageBox
import com.apexfission.android.carddetectionlite.domain.tflite.model.CardDetection

/**
 * Bounding box coordinates represented by [ImageBox].
 */
typealias BoundingBoxCoordinates = ImageBox

/**
 * Extension properties for [ImageBox] to bridge with Compose Offset, Size, and Android RectF.
 */
val ImageBox.center: Offset get() = Offset(left + intWidth / 2f, top + intHeight / 2f)
val ImageBox.topLeft: Offset get() = Offset(left.toFloat(), top.toFloat())
val ImageBox.size: Size get() = Size(intWidth.toFloat(), intHeight.toFloat())


/**
 * Lock-on progress state for card detection.
 */
@Immutable
data class LockOnProgressState(
    val lockOnProgress: Float = 0f, val smoothProgress: Float = lockOnProgress
)

/**
 * Visual animation effects state (breathe, sweep, opacity).
 */
@Immutable
data class AnimationEffectsState(
    val breathe: Float = 1f, val sweepPhase: Float = 0f, val opacity: Float = 1f
)

/**
 * Tracking metadata state (tracking status and active detection).
 */
@Immutable
data class TrackingMetadata(
    val isTracking: Boolean = false, val activeDetection: CardDetection? = null
)

/**
 * Animated screen coordinates and tracking state for card detection overlays,
 * utilizing [ImageBox] for pixel coordinates and sub-data classes for progress, effects, and tracking.
 */
@Immutable
data class AnimatedDetectionBounds(
    val coordinates: BoundingBoxCoordinates,
    val progress: LockOnProgressState = LockOnProgressState(),
    val effects: AnimationEffectsState = AnimationEffectsState(),
    val tracking: TrackingMetadata = TrackingMetadata()
) {
    constructor(
        left: Number,
        top: Number,
        right: Number,
        bottom: Number,
        lockOnProgress: Float = 0f,
        smoothProgress: Float = lockOnProgress,
        breathe: Float = 1f,
        sweepPhase: Float = 0f,
        opacity: Float = 1f,
        isTracking: Boolean = false,
        activeDetection: CardDetection? = null
    ) : this(
        coordinates = BoundingBoxCoordinates.from2P(left.toInt(), top.toInt(), right.toInt(), bottom.toInt()),
        progress = LockOnProgressState(lockOnProgress, smoothProgress),
        effects = AnimationEffectsState(breathe, sweepPhase, opacity),
        tracking = TrackingMetadata(isTracking, activeDetection)
    )

    val left: Float get() = coordinates.left.toFloat()
    val top: Float get() = coordinates.top.toFloat()
    val right: Float get() = coordinates.right.toFloat()
    val bottom: Float get() = coordinates.bottom.toFloat()

    val lockOnProgress: Float get() = progress.lockOnProgress
    val smoothProgress: Float get() = progress.smoothProgress

    val breathe: Float get() = effects.breathe
    val sweepPhase: Float get() = effects.sweepPhase
    val opacity: Float get() = effects.opacity

    val isTracking: Boolean get() = tracking.isTracking
    val activeDetection: CardDetection? get() = tracking.activeDetection

    val width: Float get() = coordinates.width.toFloat()
    val height: Float get() = coordinates.height.toFloat()
    val center: Offset get() = coordinates.center
    val topLeft: Offset get() = coordinates.topLeft
    val size: Size get() = coordinates.size
}

/**
 * Configuration for animated detection tracking overlays.
 */
@Immutable
data class DetectionAnimationConfig(
    val idleOpacity: Float = 0.35f,
    val detectedOpacity: Float = 0.85f,
    val trackingAnimationDurationMs: Int = 200,
    val resetAnimationDurationMs: Int = 300,
    val fadeAnimationDurationMs: Int = 1_000,
    val resetDetectionIndicatorTime: Long = 350,
    val enableGuideSmoothing: Boolean = true,
    val resetPositionOnMissing: Boolean = true,
    val enableContinuousAnimations: Boolean = true,
)
