package com.apexfission.android.carddetectionlite.ui

import android.app.Application
import android.os.SystemClock
import android.util.Log
import androidx.camera.core.CameraControl
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageProxy
import androidx.camera.core.MeteringPoint
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.apexfission.android.carddetectionlite.domain.tflite.YoloLiteDetector
import com.apexfission.android.carddetectionlite.domain.tflite.data.RawDet
import com.apexfission.android.carddetectionlite.domain.tflite.data.DetCutout
import com.apexfission.android.carddetectionlite.domain.tflite.filters.DetectionFilter
import com.apexfission.android.carddetectionlite.domain.tflite.rotateRectToUpright
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Metadata to help the UI map detection coordinates to screen space.
 */
data class PreviewScalingInfo(
    val cropW: Float = 0f, val cropH: Float = 0f, val fullW: Float = 0f, val fullH: Float = 0f
)

class CardDetectorLiteViewModel(
    application: Application, modelPath: String, useGpu: Boolean, scoreThreshold: Float, detectionFilters: List<DetectionFilter>
) : AndroidViewModel(application) {

    private val _detections = MutableStateFlow<List<RawDet>>(emptyList())
    val detections = _detections.asStateFlow()

    private val _scalingInfo = MutableStateFlow(PreviewScalingInfo())
    val scalingInfo = _scalingInfo.asStateFlow()

    private val _detectorEnabled = MutableStateFlow(true)
    val detectorEnabled = _detectorEnabled.asStateFlow()

    private val _flashlightEnabled = MutableStateFlow(false)
    val flashlightEnabled = _flashlightEnabled.asStateFlow()

    private val detector = YoloLiteDetector(
        context = application.applicationContext,
        modelPath = modelPath,
        scoreThreshold = scoreThreshold,
        iouThreshold = 0.45f,
        useGpu = useGpu,
        detectionFilters = detectionFilters
    )

    private val lastInferMs = AtomicLong(0L)
    private val inferIntervalMs = 80L

    fun setDetectionEnabled(enabled: Boolean) {
        _detectorEnabled.value = enabled
        detector.enabled = enabled
        if (!enabled) {
            _detections.value = emptyList()
        }
    }

    fun toggleFlashlight() {
        _flashlightEnabled.value = !_flashlightEnabled.value
    }

    fun onFocusEvent(cameraControl: CameraControl, meteringPoint: MeteringPoint) {
        val action = FocusMeteringAction.Builder(meteringPoint).build()
        cameraControl.startFocusAndMetering(action)
    }

    fun processImage(imageProxy: ImageProxy, onDetections: (List<DetCutout>) -> Unit) {
        viewModelScope.launch {
            try {
                if (!_detectorEnabled.value) return@launch

                val now = SystemClock.uptimeMillis()
                if (now - lastInferMs.get() < inferIntervalMs) return@launch
                lastInferMs.set(now)

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

                val newDetections = detector.detectCutouts(imageProxy)
                _detections.value = newDetections.map { it.rawDet }
                onDetections(newDetections)

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