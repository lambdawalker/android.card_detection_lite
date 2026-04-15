package com.apexfission.android.carddetectionlite.ui

import android.app.Application
import android.graphics.Bitmap
import android.os.SystemClock
import android.util.Log
import androidx.compose.ui.unit.IntSize
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.apexfission.android.carddetectionlite.domain.tflite.detector.InputShape
import com.apexfission.android.carddetectionlite.domain.tflite.detector.YoloCardDetector
import com.apexfission.android.carddetectionlite.domain.tflite.detector.YoloDetector
import com.apexfission.android.carddetectionlite.domain.tflite.filters.CardValidator
import com.apexfission.android.carddetectionlite.domain.tflite.model.CardDetection
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CardDetectionLiteSimulatorViewModel(
    application: Application,
    modelPath: String,
    cardClasses: List<Int>,
    useGpu: Boolean,
    scoreThreshold: Float,
    cardFilters: List<CardValidator>,
    canvasSize: MutableStateFlow<IntSize>,
    imageMode: InputShape,
    private val inferenceIntervalMs: Long,
    lockOnThreshold: Int,
    numThreads: NumThreads,
) : AndroidViewModel(application) {

    private val _cardDetection = MutableStateFlow<CardDetection?>(null)
    val cardDetection: StateFlow<CardDetection?> = _cardDetection.asStateFlow()

    private val _scalingInfo = MutableStateFlow(PreviewScalingInfo())
    val scalingInfo: StateFlow<PreviewScalingInfo> = _scalingInfo.asStateFlow()

    private val detector = YoloCardDetector(
        yoloDetector = YoloDetector(
            context = application,
            modelPath = modelPath,
            scoreThreshold = scoreThreshold,
            iouThreshold = 0.45f,
            useGpu = useGpu,
            canvasSize = canvasSize,
            imageMode = imageMode,
            numThreads = numThreads
        ),
        cardValidators = cardFilters,
        cardClasses = cardClasses,
        lockOnThreshold = lockOnThreshold
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

                _scalingInfo.value = PreviewScalingInfo(
                    cropW = bitmap.width.toFloat(),
                    cropH = bitmap.height.toFloat(),
                    fullW = bitmap.width.toFloat(),
                    fullH = bitmap.height.toFloat()
                )

                val card = detector.extractCard(bitmap)

                Log.d("YOLO_SIM", "Card: ${card != null}")

                if (card == null) {
                    _cardDetection.value = null
                    return@launch
                }

                _cardDetection.value = card
                onDetection(card)
            } catch (t: Throwable) {
                Log.e("YOLO_SIM", "Inference failed", t)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        detector.close()
    }
}
