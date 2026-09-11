package com.apexfission.android.carddetectionlite.ui.simulation

import android.graphics.Bitmap
import android.os.SystemClock
import android.util.Log
import androidx.compose.ui.unit.IntSize
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.apexfission.android.carddetectionlite.domain.tflite.detector.CardDetector
import com.apexfission.android.carddetectionlite.domain.tflite.detector.PreProcessingImageTransformation
import com.apexfission.android.carddetectionlite.domain.tflite.detector.YoloDetector
import com.apexfission.android.carddetectionlite.domain.tflite.filters.CardValidator
import com.apexfission.android.carddetectionlite.domain.tflite.image.cropWithOffset
import com.apexfission.android.carddetectionlite.domain.tflite.model.CardDetection
import com.apexfission.android.carddetectionlite.ui.detector.NumThreads
import com.apexfission.android.carddetectionlite.ui.detector.SingleFlightGate
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for [CardTrackingSimulator].
 */
class CardTrackingSimulatorViewModel(
    application: android.app.Application,
    modelPath: String,
    val classLabels: Map<Int, String> = emptyMap(),
    cardClasses: Set<Int>,
    useGpu: Boolean,
    scoreThreshold: Float,
    iouThreshold: Float,
    cardFilters: List<CardValidator>,
    private val inferenceIntervalMs: Long,
    lockOnThreshold: Int,
    noDetectionCountLimit: Int,
    memoryDetectionTimeLimit: Long,
    validateClassIdInLockOnProcess: Boolean,
    differenceHashDistanceLimit: Int,
    allowTemporalDrift: Boolean,
    private val preProcessingImageTransformation: PreProcessingImageTransformation,
    numThreads: NumThreads,
) : AndroidViewModel(application) {

    private val _cardDetection = MutableStateFlow<CardDetection?>(null)
    val cardDetection = _cardDetection.asStateFlow()

    private val _latestValidDetection = MutableStateFlow<CardDetection?>(null)
    val latestValidDetection = _latestValidDetection.asStateFlow()

    private val detector = CardDetector(
        yoloDetector = YoloDetector(
            context = application,
            modelPath = modelPath,
            scoreThreshold = scoreThreshold,
            iouThreshold = iouThreshold,
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
    private val frameProcessingGate = SingleFlightGate()

    fun setDetectionEnabled(enabled: Boolean) {
        detector.enabled = enabled
        if (!enabled) {
            _cardDetection.value = null
        }
    }

    fun processBitmap(bitmap: Bitmap, onDetection: (CardDetection) -> Unit) {
        if (!detector.enabled) return

        // Video frame callbacks can arrive faster than inference completes. Drop the
        // incoming frame instead of launching overlapping or backlogged work.
        if (!frameProcessingGate.tryAcquire()) return

        val processingStarted = AtomicBoolean(false)
        val job = viewModelScope.launch(Dispatchers.Default) {
            processingStarted.set(true)
            var croppedBitmap: Bitmap? = null
            try {
                val now = SystemClock.uptimeMillis()
                if (now - lastInferenceMs.get() < inferenceIntervalMs) return@launch
                lastInferenceMs.set(now)

                val croppedResult = cropWithOffset(
                    preProcessingImageTransformation,
                    bitmap,
                    IntSize(bitmap.width, bitmap.height)
                )
                croppedBitmap = croppedResult.bitmap

                val card: CardDetection? = detector.track(croppedBitmap)

                val adjustedCard = card?.let { detection ->
                    val adjustedBox = detection.card.box.offset(croppedResult.xOffset, croppedResult.yOffset)
                    val adjustedFeatures = detection.features.map { it.copy(box = it.box.offset(croppedResult.xOffset, croppedResult.yOffset)) }
                    detection.copy(
                        card = detection.card.copy(box = adjustedBox),
                        features = adjustedFeatures
                    )
                }

                if (adjustedCard == null) {
                    _cardDetection.value = null
                    return@launch
                }

                _cardDetection.value = adjustedCard
                _latestValidDetection.value = adjustedCard
                onDetection(adjustedCard)

            } catch (t: Throwable) {
                Log.e("Simulation", "Processing failed", t)
            } finally {
                if (croppedBitmap != null && croppedBitmap != bitmap) {
                    croppedBitmap.recycle()
                }
                frameProcessingGate.release()
            }
        }

        // Release the gate if the ViewModel scope was already cancelled and the
        // coroutine body was therefore never entered.
        job.invokeOnCompletion {
            if (!processingStarted.get()) {
                frameProcessingGate.release()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        detector.close()
    }
}
