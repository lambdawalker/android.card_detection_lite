package com.apexfission.android.carddetectionlite.ui.camerapreview

import android.app.Application
import android.os.SystemClock
import android.util.Log
import androidx.camera.core.CameraControl
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageProxy
import androidx.camera.core.MeteringPoint
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.apexfission.android.carddetectionlite.domain.tflite.detector.CardTracker
import com.apexfission.android.carddetectionlite.domain.tflite.detector.YoloDetector
import com.apexfission.android.carddetectionlite.domain.tflite.filters.CardValidator
import com.apexfission.android.carddetectionlite.domain.tflite.model.CardDetection2
import com.apexfission.android.carddetectionlite.ui.NumThreads
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * The central ViewModel for the [CardDetectorLite] screen, orchestrating the entire detection process.
 *
 * This class serves as the bridge between the UI Composables and the underlying detection engine.
 *
 * @param application The application instance, required by `AndroidViewModel`.
 * @param modelPath The asset path for the TFLite model.
 * @param cardClasses A list of class IDs that the detector should specifically treat as cards.
 * @param useGpu A flag to enable or disable the GPU delegate for TFLite.
 * @param scoreThreshold The minimum confidence for a raw detection to be considered.
 * @param cardFilters A list of [CardValidator]s to apply to potential card detections.
 * @param inferenceIntervalMs The minimum interval, in milliseconds, between consecutive inferences.
 * @param lockOnThreshold The number of consecutive frames a card must be detected and visually
 *                        similar before it is considered "locked on."
 * @param noDetectionCountLimit Number of consecutive missing detections allowed before reset.
 * @param numThreads The number of threads to use for inference on the CPU.
 */
class CardDetectorLiteViewModel(
    application: Application,
    modelPath: String,
    cardClasses: Set<Int>,
    useGpu: Boolean,
    scoreThreshold: Float,
    cardFilters: List<CardValidator>,
    private val inferenceIntervalMs: Long,
    lockOnThreshold: Int,
    noDetectionCountLimit: Int,
    numThreads: NumThreads,
) : AndroidViewModel(application) {

    private val _cardDetection = MutableStateFlow<CardDetection2?>(null)
    val cardDetection: StateFlow<CardDetection2?> = _cardDetection.asStateFlow()

    private val _flashlightEnabled = MutableStateFlow(false)
    val flashlightEnabled: StateFlow<Boolean> = _flashlightEnabled.asStateFlow()

    private val detector = CardTracker(
        yoloDetector = YoloDetector(
            context = application,
            modelPath = modelPath,
            scoreThreshold = scoreThreshold,
            iouThreshold = 0.45f,
            useGpu = useGpu,
            numThreads = numThreads
        ),
        cardValidators = cardFilters,
        cardClasses = cardClasses,
        lockOnThreshold = lockOnThreshold,
        noDetectionCountLimit = noDetectionCountLimit
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
     * @param imageProxy The frame from the camera to be processed.
     * @param onDetection A callback that will be invoked on a card detection.
     */
    fun processImage(imageProxy: ImageProxy, onDetection: (CardDetection2) -> Unit) {
        if (!detector.enabled) return

        viewModelScope.launch(Dispatchers.Default) {
            try {
                // Caps the detection frame rate
                val now = SystemClock.uptimeMillis()
                if (now - lastInferenceMs.get() < inferenceIntervalMs) return@launch
                lastInferenceMs.set(now)

                val card = detector.track(imageProxy)

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
