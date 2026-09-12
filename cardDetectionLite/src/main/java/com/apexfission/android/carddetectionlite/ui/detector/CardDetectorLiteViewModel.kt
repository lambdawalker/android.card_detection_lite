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
import java.util.concurrent.atomic.AtomicBoolean
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
    val cardDetection: StateFlow<CardDetection?> = _cardDetection.asStateFlow()

    private val detectionFrameSequencer = DetectionFrameSequencer()
    private val _detectionFrame = MutableStateFlow(DetectionFrame.Initial)
    internal val detectionFrame: StateFlow<DetectionFrame> = _detectionFrame.asStateFlow()

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
    private val latestBestDetectionStore = LatestBestDetectionStore()
    private val detectionCallbackDispatcher = LatestCallbackDispatcher<Pair<CardDetection, Bitmap>>(
        scope = viewModelScope,
        dispatcher = Dispatchers.Default,
        releaseUndelivered = { (_, bitmap) -> bitmap.recycle() },
        onCallbackFailure = { throwable ->
            Log.e("CardDetectorCallback", "onCardDetection failed", throwable)
        },
    )

    fun setDetectionEnabled(enabled: Boolean) {
        detector.enabled = enabled

        if (!enabled) {
            _cardDetection.value = null
            _detectionFrame.value = detectionFrameSequencer.reset()
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
        onDetection: (CardDetection, Bitmap) -> Unit,
    ) {
        if (!detector.enabled) {
            imageProxy.close()
            return
        }

        if (!frameProcessingGate.tryAcquire()) {
            imageProxy.close()
            return
        }

        val processingStarted = AtomicBoolean(false)
        val job = viewModelScope.launch(Dispatchers.Default) {
            processingStarted.set(true)
            imageProxy.use { proxy ->
                var uprightBitmap: Bitmap? = null
                var croppedBitmap: Bitmap? = null
                var deliveryBitmap: Bitmap? = null
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
                        publishDetectionFrame(null)
                        return@launch
                    }

                    val detectedCardBitmap = croppedBitmap.crop(card.card.box)
                    deliveryBitmap = detectedCardBitmap
                    if (latestBestDetectionStore.offer(adjustedCard, detectedCardBitmap)) {
                        _latestBestDetection.value = adjustedCard
                    }
                    publishDetectionFrame(adjustedCard)
                    detectionCallbackDispatcher.submit(adjustedCard to detectedCardBitmap) { (detection, bitmap) ->
                        onDetection(detection, bitmap)
                    }
                    deliveryBitmap = null
                } catch (t: Throwable) {
                    Log.e("YOLO", "Inference failed", t)
                } finally {
                    if (croppedBitmap != null && croppedBitmap != uprightBitmap) {
                        croppedBitmap.recycle()
                    }
                    deliveryBitmap?.recycle()
                    uprightBitmap?.recycle()
                    frameProcessingGate.release()
                }
            }
        }

        job.invokeOnCompletion {
            if (!processingStarted.get()) {
                imageProxy.close()
                frameProcessingGate.release()
            }
        }
    }

    private fun publishDetectionFrame(detection: CardDetection?) {
        _cardDetection.value = detection
        _detectionFrame.value = detectionFrameSequencer.next(detection)
    }

    suspend fun captureLatest(
        onCapture: (CardDetection, Bitmap) -> Unit,
    ): Boolean = latestBestDetectionStore.withCopy(onCapture)

    override fun onCleared() {
        super.onCleared()
        detectionCallbackDispatcher.close()
        latestBestDetectionStore.clear()
        detector.close()
    }
}
