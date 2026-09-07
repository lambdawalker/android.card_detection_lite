package com.apexfission.android.carddetectionlite.ui.simulation

import android.app.Application
import android.graphics.Bitmap
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.apexfission.android.carddetectionlite.domain.tflite.detector.CardDetector
import com.apexfission.android.carddetectionlite.domain.tflite.detector.YoloDetector
import com.apexfission.android.carddetectionlite.domain.tflite.filters.CardValidator
import com.apexfission.android.carddetectionlite.domain.tflite.model.CardDetection
import com.apexfission.android.carddetectionlite.ui.detector.NumThreads
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for [CardTrackingSimulator].
 *
 * @param application Application instance.
 * @param modelPath Asset path to TFLite model.
 * @param cardClasses Set of card class IDs.
 * @param useGpu Whether GPU inference is enabled.
 * @param scoreThreshold Score threshold for raw detections.
 * @param cardFilters List of card validators.
 * @param inferenceIntervalMs Minimum time interval between inferences.
 * @param lockOnThreshold Consistent frame threshold for lock-on.
 * @param noDetectionCountLimit Limit of missing frames before tracking resets.
 * @param numThreads CPU thread configuration.
 */
class CardTrackingSimulatorViewModel(
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
    val cardDetection = _cardDetection.asStateFlow()

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

    fun setDetectionEnabled(enabled: Boolean) {
        detector.enabled = enabled
        if (!enabled) {
            _cardDetection.value = null
        }
    }

    fun processBitmap(bitmap: Bitmap, onDetection: (CardDetection) -> Unit) {
        if (!detector.enabled) return

        viewModelScope.launch(Dispatchers.Default) {
            try {
                val now = SystemClock.uptimeMillis()
                if (now - lastInferenceMs.get() < inferenceIntervalMs) return@launch
                lastInferenceMs.set(now)

                val card: CardDetection? = detector.track(bitmap)

                if (card == null) {
                    _cardDetection.value = null
                    return@launch
                }

                _cardDetection.value = card
                onDetection(card)

            } catch (_: Throwable) {

            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        detector.close()
    }
}
