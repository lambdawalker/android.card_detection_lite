package com.apexfission.android.carddetectionlite.ui.detector

import android.app.Application
import android.graphics.Bitmap
import android.os.SystemClock
import android.util.Log
import androidx.camera.core.CameraControl
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageProxy
import androidx.camera.core.MeteringPoint
import androidx.compose.ui.unit.IntSize
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.apexfission.android.carddetectionlite.domain.tflite.detector.CardDetector
import com.apexfission.android.carddetectionlite.domain.tflite.detector.PreProcessingImageTransformation
import com.apexfission.android.carddetectionlite.domain.tflite.detector.YoloDetector
import com.apexfission.android.carddetectionlite.domain.tflite.filters.CardValidator
import com.apexfission.android.carddetectionlite.domain.tflite.image.cropWithOffset
import com.apexfission.android.carddetectionlite.domain.tflite.image.crop
import com.apexfission.android.carddetectionlite.domain.tflite.image.toUprightBitmap
import com.apexfission.android.carddetectionlite.domain.tflite.model.CardDetection
import com.apexfission.android.carddetectionlite.resource.BitmapTransfer
import com.apexfission.android.carddetectionlite.resource.withBitmapTransfer
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * The central ViewModel for the [CardDetectorLite] screen, orchestrating the card tracking process.
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
    private val preProcessingImageTransformation: PreProcessingImageTransformation,
    numThreads: NumThreads,
) : AndroidViewModel(application) {

    private val _cardDetection = MutableStateFlow<CardDetection?>(null)
    val cardDetection: StateFlow<CardDetection?> = _cardDetection.asStateFlow()

    private val _latestBestDetection = MutableStateFlow<CardDetection?>(null)
    val latestBestDetection: StateFlow<CardDetection?> = _latestBestDetection.asStateFlow()

    private val _flashlightEnabled = MutableStateFlow(false)
    val flashlightEnabled: StateFlow<Boolean> = _flashlightEnabled.asStateFlow()

    private val _flashlightAvailable = MutableStateFlow(true)
    val flashlightAvailable: StateFlow<Boolean> = _flashlightAvailable.asStateFlow()

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
    private val latestBestDetectionStore = LatestBestDetectionStore()

    fun setDetectionEnabled(enabled: Boolean) {
        detector.enabled = enabled

        if (!enabled) {
            _cardDetection.value = null
        }
    }

    fun setFlashlightAvailable(available: Boolean) {
        _flashlightAvailable.value = available
        if (!available) {
            _flashlightEnabled.value = false
        }
    }

    fun toggleFlashlight() {
        if (_flashlightAvailable.value) {
            _flashlightEnabled.value = !_flashlightEnabled.value
        }
    }

    fun onFocusEvent(cameraControl: CameraControl, meteringPoint: MeteringPoint) {
        cameraControl.startFocusAndMetering(FocusMeteringAction.Builder(meteringPoint).build())
    }

    fun processImage(
        imageProxy: ImageProxy,
        onDetection: (CardDetection, BitmapTransfer) -> Unit,
    ) {
        if (!detector.enabled) {
            imageProxy.close()
            return
        }

        viewModelScope.launch(Dispatchers.Default) {
            imageProxy.use { proxy ->
                var uprightBitmap: Bitmap? = null
                var croppedBitmap: Bitmap? = null
                try {
                    val now = SystemClock.uptimeMillis()
                    if (now - lastInferenceMs.get() < inferenceIntervalMs) return@launch
                    lastInferenceMs.set(now)

                    uprightBitmap = proxy.toUprightBitmap()
                    val croppedResult = cropWithOffset(
                        preProcessingImageTransformation,
                        uprightBitmap,
                        IntSize(uprightBitmap.width, uprightBitmap.height)
                    )
                    croppedBitmap = croppedResult.bitmap

                    val card = detector.track(croppedBitmap)

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

                    val sourceCard = checkNotNull(card)
                    val detectedCardBitmap = croppedBitmap.crop(sourceCard.card.box)
                    withBitmapTransfer(detectedCardBitmap) { transfer ->
                        if (latestBestDetectionStore.offer(adjustedCard, detectedCardBitmap)) {
                            _latestBestDetection.value = adjustedCard
                        }
                        _cardDetection.value = adjustedCard
                        onDetection(adjustedCard, transfer)
                    }
                } catch (t: Throwable) {
                    Log.e("YOLO", "Inference failed", t)
                } finally {
                    if (croppedBitmap != null && croppedBitmap != uprightBitmap) {
                        croppedBitmap.recycle()
                    }
                    uprightBitmap?.recycle()
                }
            }
        }
    }

    fun captureLatest(
        onCapture: (CardDetection, BitmapTransfer) -> Unit,
    ): Boolean = latestBestDetectionStore.withTransfer(onCapture)

    override fun onCleared() {
        super.onCleared()
        latestBestDetectionStore.clear()
        detector.close()
    }
}
