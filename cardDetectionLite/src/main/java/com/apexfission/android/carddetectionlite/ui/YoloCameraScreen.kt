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

class YoloCameraViewModelFactory(
    private val application: Application, private val modelName: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(YoloCameraViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST") return YoloCameraViewModel(application, modelName) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

@Composable
fun YoloCameraScreen(
    modelName: String,
    isDetectionEnabled: Boolean,
    modifier: Modifier = Modifier,
    showBoundingBoxes: Boolean = true,
    showClassNames: Boolean = true,
    classLabels: Map<Int, String> = emptyMap(),
    onDetections: (List<Bitmap>) -> Unit
) {
    val context = LocalContext.current
    val viewModel: YoloCameraViewModel = viewModel(
        factory = YoloCameraViewModelFactory(context.applicationContext as Application, modelName)
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

        Switch(
            checked = flashlightEnabled,
            onCheckedChange = { viewModel.toggleFlashlight() },
            modifier = Modifier.padding(16.dp)
        )
    }
}