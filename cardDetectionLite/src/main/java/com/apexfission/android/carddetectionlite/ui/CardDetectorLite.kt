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
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.apexfission.android.carddetectionlite.domain.tflite.detector.InputShape
import com.apexfission.android.carddetectionlite.domain.tflite.filters.AspectRatioValidator
import com.apexfission.android.carddetectionlite.domain.tflite.filters.CardValidator
import com.apexfission.android.carddetectionlite.domain.tflite.filters.MarginValidator
import com.apexfission.android.carddetectionlite.domain.tflite.model.CardDetection
import kotlinx.coroutines.flow.MutableStateFlow


@Composable
fun CardDetectorLite(
    modelPath: String,
    classLabels: Map<Int, String>,
    cardClasses: List<Int>,
    isDetectionEnabled: Boolean,
    modifier: Modifier = Modifier,
    useGpu: Boolean = true,
    showBoundingBoxes: Boolean = false,
    showClassNames: Boolean = false,
    showFlashlightSwitch: Boolean = true,
    showLockOnProgress: Boolean = true,
    scoreThreshold: Float = 0.70f,
    analysisTargetResolution: Size = Size(1920, 1080),
    cardCardFilters: List<CardValidator> = listOf(
        MarginValidator(), AspectRatioValidator()
    ),
    onCardDetection: (CardDetection) -> Unit,
    imageMode: InputShape = InputShape.SquareCrop
) {
    val context = LocalContext.current
    val sizeInPixels = MutableStateFlow(IntSize.Zero)

    val viewModel: CardDetectorLiteViewModel = viewModel(
        factory = CardDetectorLiteViewModelFactory(
            application = context.applicationContext as Application,
            modelPath = modelPath,
            cardClasses = cardClasses,
            useGpu = useGpu,
            scoreThreshold = scoreThreshold,
            cardFilters = cardCardFilters,
            canvasSize = sizeInPixels,
            imageMode = imageMode,
        )
    )

    LaunchedEffect(isDetectionEnabled) {
        viewModel.setDetectionEnabled(isDetectionEnabled)
    }

    val flashlightEnabled by viewModel.flashlightEnabled.collectAsStateWithLifecycle()
    val cardDetection by viewModel.cardDetection.collectAsStateWithLifecycle()
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
            viewModel.processImage(imageProxy, onCardDetection)
        }, onFocusEvent = { cameraControl, meteringPoint ->
            viewModel.onFocusEvent(
                cameraControl, meteringPoint
            )
        }, flashlightEnabled = flashlightEnabled, analysisTargetResolution = analysisTargetResolution, focusOn = cardDetection
        )

        if (isDetectionEnabled && showBoundingBoxes && scalingInfo.fullW > 0) {
            DetectionOverlay(
                cardDetection = cardDetection, scalingInfo = scalingInfo, showClassNames = showClassNames, classLabels = classLabels
            )
        }

        if (isDetectionEnabled && showLockOnProgress && scalingInfo.fullW > 0) {
            CardLockOnOverlay(
                activeDetection = cardDetection, scalingInfo = scalingInfo
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
