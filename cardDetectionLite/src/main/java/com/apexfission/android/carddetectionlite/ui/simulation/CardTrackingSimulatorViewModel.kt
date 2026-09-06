package com.apexfission.android.carddetectionlite.ui.simulation

import android.app.Application
import android.graphics.Bitmap
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.apexfission.android.carddetectionlite.domain.tflite.detector.CardTracker
import com.apexfission.android.carddetectionlite.domain.tflite.detector.YoloDetector2
import com.apexfission.android.carddetectionlite.domain.tflite.filters.CardValidator2
import com.apexfission.android.carddetectionlite.domain.tflite.model.CardDetection2
import com.apexfission.android.carddetectionlite.ui.NumThreads
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CardTrackingSimulatorViewModel(
    application: Application,
    modelPath: String,
    cardClasses: Set<Int>,
    useGpu: Boolean,
    scoreThreshold: Float,
    cardFilters: List<CardValidator2>,
    private val inferenceIntervalMs: Long,
    lockOnThreshold: Int,
    noDetectionCountLimit: Int,
    numThreads: NumThreads,
) : AndroidViewModel(application) {

    private val _cardDetection = MutableStateFlow<CardDetection2?>(null)
    val cardDetection = _cardDetection.asStateFlow()

    private val detector = CardTracker(
        yoloDetector = YoloDetector2(
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
    )

    private val lastInferenceMs = AtomicLong(0L)

    fun setDetectionEnabled(enabled: Boolean) {
        detector.enabled = enabled
        if (!enabled) {
            _cardDetection.value = null
        }
    }

    fun processBitmap(bitmap: Bitmap, onDetection: (CardDetection2) -> Unit) {
        if (!detector.enabled) return

        viewModelScope.launch(Dispatchers.Default) {
            try {
                val now = SystemClock.uptimeMillis()
                if (now - lastInferenceMs.get() < inferenceIntervalMs) return@launch
                lastInferenceMs.set(now)


                val card: CardDetection2? = detector.track(bitmap)

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
