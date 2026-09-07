package com.apexfission.android.carddetectionlite.ui.detector

import android.app.Application
import android.os.SystemClock
import android.util.Log
import androidx.camera.core.CameraControl
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageProxy
import androidx.camera.core.MeteringPoint
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.apexfission.android.carddetectionlite.domain.tflite.detector.CardDetector
import com.apexfission.android.carddetectionlite.domain.tflite.detector.YoloDetector
import com.apexfission.android.carddetectionlite.domain.tflite.filters.CardValidator
import com.apexfission.android.carddetectionlite.domain.tflite.model.CardDetection
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * The central ViewModel for the [CardDetectorLite] screen, orchestrating the card tracking process.
 *
 * This class serves as the bridge between the UI Composables and the underlying [CardDetector].
 * Its responsibilities include:
 * - Owning and initializing [CardDetector] and [YoloDetector].
 * - Receiving image frames from [com.apexfission.android.carddetectionlite.ui.camerapreview.CameraPreview].
 * - Throttling inference rate to maintain smooth UI performance.
 * - Dispatching inference work to background threads.
 * - Exposing state flows for detection results and flashlight state.
 *
 * @param application The application instance.
 * @param modelPath The asset path for the TFLite model.
 * @param cardClasses A set of class IDs that the detector should specifically treat as primary card targets.
 * @param useGpu A flag to enable or disable GPU acceleration for inference.
 * @param scoreThreshold The minimum confidence for a raw detection candidate to be considered.
 * @param cardFilters A list of [CardValidator]s to apply to candidate detections.
 * @param inferenceIntervalMs The minimum interval, in milliseconds, between consecutive inferences.
 * @param lockOnThreshold The number of consecutive frames a card must be detected and visually similar before locking.
 * @param noDetectionCountLimit Number of consecutive missing detections allowed before resetting tracking state.
 * @param numThreads CPU thread configuration using [NumThreads].
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
    memoryDetectionTimeLimit: Long,
    validateClassIdInLockOnProcess: Boolean,
    differenceHashDistanceLimit: Int,
    allowTemporalDrift: Boolean,
    numThreads: NumThreads,
) : AndroidViewModel(application) {

    private val _cardDetection = MutableStateFlow<CardDetection?>(null)
    val cardDetection: StateFlow<CardDetection?> = _cardDetection.asStateFlow()

    private val _flashlightEnabled = MutableStateFlow(false)
    val flashlightEnabled: StateFlow<Boolean> = _flashlightEnabled.asStateFlow()

    private val detector = CardDetector(
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
        noDetectionCountLimit = noDetectionCountLimit,
        memoryDetectionTimeLimit = memoryDetectionTimeLimit,
        validateClassIdInLockOnProcess = validateClassIdInLockOnProcess,
        differenceHashDistanceLimit = differenceHashDistanceLimit,
        allowTemporalDrift = allowTemporalDrift
    )

    private val lastInferenceMs = AtomicLong(0L)

    /** Toggles the detection process on or off. When disabled, incoming frames are ignored. */
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
     * @param onDetection A callback that will be invoked on a card detection event.
     */
    fun processImage(imageProxy: ImageProxy, onDetection: (CardDetection) -> Unit) {
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
