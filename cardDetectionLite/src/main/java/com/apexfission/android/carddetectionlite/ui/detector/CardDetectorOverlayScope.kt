package com.apexfission.android.carddetectionlite.ui.detector

import androidx.compose.runtime.Stable
import com.apexfission.android.carddetectionlite.domain.coordinates.models.ImageSpaceChain
import com.apexfission.android.carddetectionlite.domain.tflite.model.CardDetection
import com.apexfission.android.carddetectionlite.ui.camerapreview.CameraPreset

/**
 * Scoped interface provided to custom control overlays in [CardDetectorLite].
 *
 * Exposes real-time detection state, flashlight state, presets, and user actions without
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
     * The highest-confidence [CardDetection] retained for the current card.
     */
    val latestBestDetection: CardDetection?

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
     * The active ML pipeline configuration preset ([CardDetectorPreset]).
     */
    val detectorPreset: CardDetectorPreset

    /**
     * The active camera lens and focus configuration preset ([CameraPreset]).
     */
    val cameraPreset: CameraPreset

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
    override val latestBestDetection: CardDetection?,
    override val imageSpaceChain: ImageSpaceChain?,
    override val flashlightAvailable: Boolean,
    override val flashlightEnabled: Boolean,
    override val detectorPreset: CardDetectorPreset = CardDetectorPreset.HighPerformance,
    override val cameraPreset: CameraPreset = CameraPreset.Default,
    private val onCaptureRequested: () -> Unit,
    private val onBackRequested: () -> Unit,
    private val onFlashlightToggleRequested: () -> Unit
) : CardDetectorOverlayScope {

    override val captureEnabled: Boolean
        get() = latestBestDetection != null

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
