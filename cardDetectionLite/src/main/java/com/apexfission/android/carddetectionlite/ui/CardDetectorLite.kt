package com.apexfission.android.carddetectionlite.ui

import android.app.Application
import android.util.Size
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.apexfission.android.carddetectionlite.domain.tflite.YoloLiteDetector
import com.apexfission.android.carddetectionlite.domain.tflite.data.Detection
import com.apexfission.android.carddetectionlite.domain.tflite.filters.AspectRatioFilter
import com.apexfission.android.carddetectionlite.domain.tflite.filters.DetectionFilter
import com.apexfission.android.carddetectionlite.domain.tflite.filters.MarginFilter
import kotlinx.coroutines.flow.MutableStateFlow

class CardDetectorLiteViewModelFactory(
    private val application: Application,
    private val modelPath: String,
    private val useGpu: Boolean,
    private val scoreThreshold: Float,
    private val detectionFilters: List<DetectionFilter>,
    private val canvasSize: MutableStateFlow<IntSize>,
    private val imageMode: YoloLiteDetector.InputShape
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CardDetectorLiteViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST") return CardDetectorLiteViewModel(
                application, modelPath, useGpu, scoreThreshold, detectionFilters, canvasSize, imageMode
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

@Composable
fun CardDetectorLite(
    modelPath: String,
    classLabels: Map<Int, String>,
    isDetectionEnabled: Boolean,
    modifier: Modifier = Modifier,
    useGpu: Boolean = true,
    showBoundingBoxes: Boolean = true,
    showClassNames: Boolean = true,
    showFlashlightSwitch: Boolean = true,
    scoreThreshold: Float = 0.70f,
    analysisTargetResolution: Size = Size(1920, 1080),
    detectionFilters: List<DetectionFilter> = listOf(
        MarginFilter(), AspectRatioFilter()
    ),
    onDetections: (List<Detection>) -> Unit,
    imageMode: YoloLiteDetector.InputShape = YoloLiteDetector.InputShape.SquareCrop
) {
    val context = LocalContext.current


    val sizeInPixels = MutableStateFlow(IntSize.Zero)

    val viewModel: CardDetectorLiteViewModel = viewModel(
        factory = CardDetectorLiteViewModelFactory(
            application = context.applicationContext as Application,
            modelPath = modelPath,
            useGpu = useGpu,
            scoreThreshold = scoreThreshold,
            detectionFilters = detectionFilters,
            canvasSize = sizeInPixels,
            imageMode = imageMode
        )
    )

    LaunchedEffect(isDetectionEnabled) {
        viewModel.setDetectionEnabled(isDetectionEnabled)
    }

    val flashlightEnabled by viewModel.flashlightEnabled.collectAsStateWithLifecycle()
    val detections by viewModel.detections.collectAsStateWithLifecycle()
    val scalingInfo by viewModel.scalingInfo.collectAsStateWithLifecycle()

    Box(
        modifier
            .fillMaxSize()
            .onSizeChanged { size ->
                // size.width and size.height are in pixels
                sizeInPixels.value = size
            }) {
        CameraPreview(
            lifecycleOwner = LocalLifecycleOwner.current, onFrame = { imageProxy ->
                viewModel.processImage(imageProxy, onDetections)
            }, onFocusEvent = { cameraControl, meteringPoint ->
                viewModel.onFocusEvent(
                    cameraControl, meteringPoint
                )
            }, flashlightEnabled = flashlightEnabled, analysisTargetResolution = analysisTargetResolution
        )

        if (isDetectionEnabled && showBoundingBoxes && scalingInfo.fullW > 0) {
            DetectionOverlay(
                detections = detections, scalingInfo = scalingInfo, showClassNames = showClassNames, classLabels = classLabels
            )
        }

        if (showFlashlightSwitch) {
            IconButton(
                onClick = { viewModel.toggleFlashlight() }, modifier = Modifier.padding(16.dp)
            ) {
                Icon(
                    imageVector = if (flashlightEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff, contentDescription = "Toggle Flashlight"
                )
            }
        }
    }
}