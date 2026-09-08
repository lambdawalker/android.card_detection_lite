package com.apexfission.android.carddetectionlite.ui.overlays

import android.graphics.RectF
import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import com.apexfission.android.carddetectionlite.domain.tflite.model.CardDetection

/**
 * Animated screen coordinates and tracking state for card detection overlays.
 *
 * @property left Left coordinate in overlay screen pixels.
 * @property top Top coordinate in overlay screen pixels.
 * @property right Right coordinate in overlay screen pixels.
 * @property bottom Bottom coordinate in overlay screen pixels.
 * @property lockOnProgress Raw lock-on progress from 0.0f (locking) to 1.0f (locked).
 * @property smoothProgress Spring-animated lock-on progress from 0.0f to 1.0f.
 * @property breathe Oscillating pulse scale value between 0.96f and 1.04f for breathing animation.
 * @property sweepPhase Continuous linear phase value between 0.0f and 1.0f for sweeping glow animation.
 * @property opacity Animated opacity level from 0.0f to 1.0f.
 * @property isTracking Whether a card is actively detected in the current stream.
 * @property activeDetection The active or last detected [CardDetection] instance.
 */
@Immutable
data class AnimatedDetectionBounds(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
    val lockOnProgress: Float = 0f,
    val smoothProgress: Float = lockOnProgress,
    val breathe: Float = 1f,
    val sweepPhase: Float = 0f,
    val opacity: Float = 1f,
    val isTracking: Boolean = false,
    val activeDetection: CardDetection? = null
) {
    /**
     * Width of the bounding box in overlay screen pixels.
     */
    val width: Float get() = (right - left).coerceAtLeast(0f)

    /**
     * Height of the bounding box in overlay screen pixels.
     */
    val height: Float get() = (bottom - top).coerceAtLeast(0f)

    /**
     * Center coordinate of the bounding box in overlay screen pixels.
     */
    val center: Offset get() = Offset(left + width / 2f, top + height / 2f)

    /**
     * Top-left origin offset in overlay screen pixels.
     */
    val topLeft: Offset get() = Offset(left, top)

    /**
     * Size of the bounding box in overlay screen pixels.
     */
    val size: Size get() = Size(width, height)

    /**
     * Bounding box as an [android.graphics.RectF].
     */
    val rectF: RectF get() = RectF(left, top, right, bottom)
}

/**
 * Configuration for animated detection tracking overlays.
 *
 * @property idleOpacity Opacity level when no card is detected. Defaults to 0.35f (35%).
 * @property detectedOpacity Opacity level when a card is detected. Defaults to 0.85f (85%).
 * @property missingCardResetDelayMs Time in milliseconds without a card detection before resetting. Defaults to 2,000ms.
 * @property maxConsecutiveMisses Optional threshold for consecutive frames without a detection before resetting.
 * @property trackingAnimationDurationMs Duration in milliseconds for position/size animation while tracking. Defaults to 200ms.
 * @property resetAnimationDurationMs Duration in milliseconds for reset animation back to center. Defaults to 300ms.
 * @property fadeAnimationDurationMs Duration in milliseconds for opacity fade animation when card is missing. Defaults to 1,000ms.
 * @property enableGuideSmoothing Whether to enable coordinate transition smoothing. Defaults to true.
 * @property resetPositionOnMissing Whether to animate bounds back to default center when missing (`true`), or leave bounds at last known detected position (`false`). Defaults to `true`.
 */
@Immutable
data class DetectionAnimationConfig(
    val idleOpacity: Float = 0.35f,
    val detectedOpacity: Float = 0.85f,
    val missingCardResetDelayMs: Long = 2_000L,
    val maxConsecutiveMisses: Int? = null,
    val trackingAnimationDurationMs: Int = 200,
    val resetAnimationDurationMs: Int = 300,
    val fadeAnimationDurationMs: Int = 1_000,
    val enableGuideSmoothing: Boolean = true,
    val resetPositionOnMissing: Boolean = true
)
