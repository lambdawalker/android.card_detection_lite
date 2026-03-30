package com.apexfission.android.carddetectionlite.ui

import android.app.Application
import android.os.SystemClock
import android.util.Log
import androidx.camera.core.CameraControl
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageProxy
import androidx.camera.core.MeteringPoint
import androidx.compose.ui.unit.IntSize
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.apexfission.android.carddetectionlite.domain.tflite.detector.InputShape
import com.apexfission.android.carddetectionlite.domain.tflite.detector.YoloCardDetector
import com.apexfission.android.carddetectionlite.domain.tflite.detector.YoloDetector
import com.apexfission.android.carddetectionlite.domain.tflite.filters.CardValidator
import com.apexfission.android.carddetectionlite.domain.tflite.image.rotateRectToUpright
import com.apexfission.android.carddetectionlite.domain.tflite.model.CardDetection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicLong

/**
 * Holds critical metadata for mapping coordinates from the model's analysis space to the UI's
 * screen space.
 *
 * @property cropW The width, in pixels, of the image that was actually analyzed by the model
 *                 (after any initial cropping but before letterboxing).
 * @property cropH The height, in pixels, of the analyzed image.
 * @property fullW The width, in pixels, of the entire camera buffer after being rotated to an
 *                 upright orientation.
 * @property fullH The height, in pixels, of the upright camera buffer.
 */
data class PreviewScalingInfo(
    val cropW: Float = 0f, val cropH: Float = 0f, val fullW: Float = 0f, val fullH: Float = 0f
)

/**
 * The central ViewModel for the [CardDetectorLite] screen, orchestrating the entire detection process.
 *
 * This class serves as the bridge between the UI Composables and the underlying detection engine.
 * Its responsibilities include:
 * - Initializing and owning the `YoloCardDetector`.
 * - Receiving image frames from the `CameraPreview`.
 * - Throttling inference to maintain a stable frame rate.
 * - Launching the detection logic on a background thread.
 * - Exposing the results and other UI-related state via `StateFlow`s.
 * - Handling user interactions like tap-to-focus and toggling the flashlight.
 *
 * @param application The application instance, required by `AndroidViewModel`.
 * @param modelPath The asset path for the TFLite model.
 * @param cardClasses A list of class IDs that the detector should specifically treat as cards.
 * @param useGpu A flag to enable or disable the GPU delegate for TFLite.
 * @param scoreThreshold The minimum confidence for a raw detection to be considered.
 * @param cardFilters A list of [CardValidator]s to apply to potential card detections.
 * @param canvasSize A `StateFlow` from the UI that provides the current size of the composable,
 *                   used for aspect-ratio calculations.
 * @param imageMode The [InputShape] configuration for image preprocessing.
 * @param inferenceIntervalMs The minimum interval, in milliseconds, between consecutive inferences.
 * @param lockOnThreshold The number of consecutive frames a card must be detected and visually
 *                        similar before it is considered "locked on."
 */
class CardDetectorLiteViewModel(
    application: Application, modelPath: String, cardClasses: List<Int>, useGpu: Boolean,
    scoreThreshold: Float, cardFilters: List<CardValidator>, canvasSize: MutableStateFlow<IntSize>,
    imageMode: InputShape, private val inferenceIntervalMs: Long, lockOnThreshold: Int,
) : AndroidViewModel(application) {

    private val _cardDetection = MutableStateFlow<CardDetection?>(null)
    val cardDetection: StateFlow<CardDetection?> = _cardDetection.asStateFlow()

    private val _scalingInfo = MutableStateFlow(PreviewScalingInfo())
    val scalingInfo: StateFlow<PreviewScalingInfo> = _scalingInfo.asStateFlow()


    private val _flashlightEnabled = MutableStateFlow(false)
    val flashlightEnabled: StateFlow<Boolean> = _flashlightEnabled.asStateFlow()

    private val detector = YoloCardDetector(
        yoloDetector = YoloDetector(application, modelPath, scoreThreshold, 0.45f, useGpu, canvasSize = canvasSize, imageMode = imageMode),
        cardValidators = cardFilters,
        cardClasses = cardClasses,
        lockOnThreshold = lockOnThreshold
    )

    private val lastInferenceMs = AtomicLong(0L)

    /** Toggles the detection process on or off. When disabled, the ViewModel will ignore incoming frames. */
    fun setDetectionEnabled(enabled: Boolean) {
        detector.enabled = enabled

        if (!enabled) {
            _cardDetection.value = null
        }
    }

    /** Toggles the state of the camera flashlight. */
    fun toggleFlashlight() {
        _flashlightEnabled.value = !_flashlightEnabled.value
    }

    /** Initiates a tap-to-focus action on the camera. */
    fun onFocusEvent(cameraControl: CameraControl, meteringPoint: MeteringPoint) {
        cameraControl.startFocusAndMetering(FocusMeteringAction.Builder(meteringPoint).build())
    }

    /**
     * The main entry point for processing a camera frame.
     *
     * This function is designed to be called rapidly from the `CameraPreview`. It contains logic
     * to throttle the rate of inference, preventing the system from being overloaded. It then
     * calculates the necessary coordinate scaling information, dispatches the detection work to a
     * background thread, and updates the public state flows with the results.
     *
     * @param imageProxy The frame from the camera to be processed.
     * @param onDetection A callback that will be invoked on a stable card detection.
     */
    fun processImage(imageProxy: ImageProxy, onDetection: (CardDetection) -> Unit) {
        if (!detector.enabled) return

        viewModelScope.launch(Dispatchers.Default) {
            try {
                // Caps the detection frame rate
                val now = SystemClock.uptimeMillis()
                if (now - lastInferenceMs.get() < inferenceIntervalMs) return@launch
                lastInferenceMs.set(now)

                val rotation = imageProxy.imageInfo.rotationDegrees
                val uprightCrop = rotateRectToUpright(
                    rect = imageProxy.cropRect, srcW = imageProxy.width, srcH = imageProxy.height, rotationDegrees = rotation
                )

                // Update scaling info: the dimensions of the crop vs the full upright buffer
                val is90or270 = rotation % 180 != 0
                val fullW = if (is90or270) imageProxy.height.toFloat() else imageProxy.width.toFloat()
                val fullH = if (is90or270) imageProxy.width.toFloat() else imageProxy.height.toFloat()

                _scalingInfo.value = PreviewScalingInfo(
                    cropW = uprightCrop.width().toFloat(), cropH = uprightCrop.height().toFloat(), fullW = fullW, fullH = fullH
                )

                val card = detector.extractCard(imageProxy)

                if (card == null) {
                    _cardDetection.value = null
                    return@launch
                }

                _cardDetection.value = card
                onDetection(card)
            } catch (t: Throwable) {
                Log.e("YOLO", "Inference failed", t)
            } finally {
                imageProxy.close()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        detector.close()
    }
}
