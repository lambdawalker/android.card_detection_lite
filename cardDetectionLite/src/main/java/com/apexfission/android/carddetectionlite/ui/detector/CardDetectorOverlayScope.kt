package com.apexfission.android.carddetectionlite.ui.detector

import androidx.compose.runtime.Stable
import com.apexfission.android.carddetectionlite.domain.coordinates.models.ImageSpaceChain
import com.apexfission.android.carddetectionlite.domain.tflite.model.CardDetection

/**
 * Scoped interface provided to custom control overlays in [CardDetectorLite].
 *
 * Exposes real-time detection state, flashlight state, and user actions without
 * exposing underlying camera or detector implementation details.
 */
@Stable
interface CardDetectorOverlayScope {
    /**
     * The active [CardDetection] from the current camera frame, or `null` if no card is detected.
     */
    val detectionState: CardDetection?

    /**
     * Convenient alias for [detectionState].
     */
    val cardDetection: CardDetection? get() = detectionState

    /**
     * The most recent valid [CardDetection] stored by the controller layer.
     * Maintained even if the current frame has no active card detection.
     */
    val latestValidDetection: CardDetection?

    /**
     * The [ImageSpaceChain] mapping card detection coordinates to screen space.
     */
    val imageSpaceChain: ImageSpaceChain?

    /**
     * Whether capture is currently enabled (e.g., when a valid detection exists).
     */
    val captureEnabled: Boolean

    /**
     * Whether the active camera supports a torch / flashlight.
     */
    val flashlightAvailable: Boolean

    /**
     * Whether the camera flashlight is currently enabled.
     */
    val flashlightEnabled: Boolean

    /**
     * Emits a user intent to capture the card detection or current frame.
     */
    fun capture()

    /**
     * Emits a user intent to navigate back.
     */
    fun goBack()

    /**
     * Emits a user intent to toggle the camera flashlight.
     */
    fun toggleFlashlight()
}

/**
 * Default implementation of [CardDetectorOverlayScope].
 */
internal class CardDetectorOverlayScopeImpl(
    override val detectionState: CardDetection?,
    override val latestValidDetection: CardDetection?,
    override val imageSpaceChain: ImageSpaceChain?,
    override val flashlightAvailable: Boolean,
    override val flashlightEnabled: Boolean,
    private val onCaptureRequested: () -> Unit,
    private val onBackRequested: () -> Unit,
    private val onFlashlightToggleRequested: () -> Unit
) : CardDetectorOverlayScope {

    override val captureEnabled: Boolean
        get() = latestValidDetection != null

    override fun capture() {
        onCaptureRequested()
    }

    override fun goBack() {
        onBackRequested()
    }

    override fun toggleFlashlight() {
        onFlashlightToggleRequested()
    }
}
