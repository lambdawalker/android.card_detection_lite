package com.apexfission.android.carddetectionlite.ui

import android.app.Application
import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

class CardDetectorLiteViewModelFactory(
    private val application: Application,
    private val modelPath: String,
    private val useGpu: Boolean,
    private val scoreThreshold: Float
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CardDetectorLiteViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST") return CardDetectorLiteViewModel(
                application,
                modelPath,
                useGpu,
                scoreThreshold
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
    onDetections: (List<Bitmap>) -> Unit
) {
    val context = LocalContext.current
    val viewModel: CardDetectorLiteViewModel = viewModel(
        factory = CardDetectorLiteViewModelFactory(
            application = context.applicationContext as Application,
            modelPath = modelPath,
            useGpu = useGpu,
            scoreThreshold = scoreThreshold
        )
    )

    LaunchedEffect(isDetectionEnabled) {
        viewModel.setDetectionEnabled(isDetectionEnabled)
    }

    val flashlightEnabled by viewModel.flashlightEnabled.collectAsStateWithLifecycle()
    val detections by viewModel.detections.collectAsStateWithLifecycle()
    val scalingInfo by viewModel.scalingInfo.collectAsStateWithLifecycle()

    Box(modifier.fillMaxSize()) {
        CameraPreview(
            lifecycleOwner = LocalLifecycleOwner.current,
            onFrame = { imageProxy -> viewModel.processImage(imageProxy, onDetections) },
            onFocusEvent = { cameraControl, meteringPoint ->
                viewModel.onFocusEvent(
                    cameraControl,
                    meteringPoint
                )
            },
            flashlightEnabled = flashlightEnabled,
        )

        if (isDetectionEnabled && showBoundingBoxes && scalingInfo.fullW > 0) {
            DetectionOverlay(
                detections = detections,
                scalingInfo = scalingInfo,
                showClassNames = showClassNames,
                classLabels = classLabels
            )
        }

        if (showFlashlightSwitch) {
            Switch(
                checked = flashlightEnabled,
                onCheckedChange = { viewModel.toggleFlashlight() },
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}