package com.apexfission.android.carddetectionlite.ui.camerapreview

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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.apexfission.android.carddetectionlite.domain.coordinates.models.ImageSpaceChain
import com.apexfission.android.carddetectionlite.domain.tflite.detector.InputShape
import com.apexfission.android.carddetectionlite.domain.tflite.filters.AspectRatioValidator
import com.apexfission.android.carddetectionlite.domain.tflite.filters.CardValidator
import com.apexfission.android.carddetectionlite.domain.tflite.filters.MarginValidator
import com.apexfission.android.carddetectionlite.domain.tflite.model.CardDetection2
import com.apexfission.android.carddetectionlite.ui.NumThreads
import com.apexfission.android.carddetectionlite.ui.simulation.SimulationDetectionOverlay
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * All-in-one Composable that provides a configurable in camera feed card detection solution.
 *
 * @param modelPath The path to the `.tflite` model file within the application's `assets` directory.
 * @param classLabels A map where keys are integer class IDs and values are human-readable string labels.
 * @param cardClasses A set of class IDs from the model that should be treated as the primary target for detection.
 * @param isDetectionEnabled A boolean flag to dynamically start or stop the detection process.
 * @param modifier A [Modifier] applied to the root `Box` of this component.
 * @param useGpu If `true`, the underlying TFLite interpreter will attempt to use the GPU delegate.
 * @param showBoundingBoxes When `true`, a detection overlay is displayed drawing boxes around detected objects.
 * @param showClassNames If `true`, labels with class name and confidence score are drawn above bounding boxes.
 * @param showFlashlightSwitch If `true`, a UI button is provided to toggle the camera flashlight.
 * @param showLockOnProgress If `true`, a lock-on overlay is displayed as the detector locks onto a card.
 * @param showDebugOverlay If `true`, a debug overlay is displayed showing current configuration parameters.
 * @param showFocusIndicator If `true`, a visual indicator is displayed where the camera is focusing.
 * @param scoreThreshold The minimum confidence score (0.0 to 1.0) a detection must have to be considered.
 * @param analysisTargetResolution The target resolution for the image analysis stream.
 * @param cardFilters A list of [CardValidator] instances used to apply additional heuristic checks.
 * @param onCardDetection A callback lambda invoked when a card is detected.
 * @param imageMode The [InputShape] configuration for image preprocessing.
 * @param inferenceIntervalMs The minimum interval, in milliseconds, between consecutive inferences.
 * @param tapToFocusEnabled A boolean flag to enable or disable tap-to-focus.
 * @param focusOnCardEnabled A boolean flag to enable or disable smart auto-focus on card.
 * @param lockOnThreshold Number of consistent frames required before lock-on.
 * @param noDetectionCountLimit Number of consecutive missing detections allowed before reset.
 * @param numThreads CPU thread configuration.
 */
@Composable
fun CardDetectorLite(
    modifier: Modifier = Modifier,
    modelPath: String,
    classLabels: Map<Int, String>,
    cardClasses: Set<Int>,
    isDetectionEnabled: Boolean,
    useGpu: Boolean = true,
    showBoundingBoxes: Boolean = false,
    showClassNames: Boolean = false,
    showFlashlightSwitch: Boolean = true,
    showLockOnProgress: Boolean = true,
    showDebugOverlay: Boolean = false,
    showFocusIndicator: Boolean = true,
    scoreThreshold: Float = 0.65f,
    analysisTargetResolution: Size = Size(2048, 1080),
    cardFilters: List<CardValidator> = listOf(
        MarginValidator(), AspectRatioValidator()
    ),
    onCardDetection: (CardDetection2) -> Unit,
    imageMode: InputShape = InputShape.SquareCrop,
    inferenceIntervalMs: Long = 33L,
    tapToFocusEnabled: Boolean = true,
    focusOnCardEnabled: Boolean = true,
    lockOnThreshold: Int = 4,
    noDetectionCountLimit: Int = 8,
    numThreads: NumThreads = NumThreads.Default
) {
    val context = LocalContext.current
    val imageSpaceChainFlow = remember { MutableStateFlow<ImageSpaceChain?>(null) }

    val viewModel: CardDetectorLiteViewModel = viewModel(
        factory = CardDetectorLiteViewModelFactory(
            application = context.applicationContext as Application,
            modelPath = modelPath,
            cardClasses = cardClasses,
            useGpu = useGpu,
            scoreThreshold = scoreThreshold,
            cardFilters = cardFilters,
            inferenceIntervalMs = inferenceIntervalMs,
            lockOnThreshold = lockOnThreshold,
            noDetectionCountLimit = noDetectionCountLimit,
            numThreads = numThreads,
        )
    )

    LaunchedEffect(isDetectionEnabled) {
        viewModel.setDetectionEnabled(isDetectionEnabled)
    }

    val flashlightEnabled by viewModel.flashlightEnabled.collectAsStateWithLifecycle()
    val cardDetection by viewModel.cardDetection.collectAsStateWithLifecycle()
    val imageSpaceChain by imageSpaceChainFlow.collectAsStateWithLifecycle()

    Box(modifier = modifier.fillMaxSize()) {

        CameraPreview(
            lifecycleOwner = LocalLifecycleOwner.current,
            onFrame = { imageProxy, spaceChain ->
                imageSpaceChainFlow.value = spaceChain
                viewModel.processImage(imageProxy, onCardDetection)
            },
            onFocusEvent = viewModel::onFocusEvent,
            flashlightEnabled = flashlightEnabled,
            analysisTargetResolution = analysisTargetResolution,
            focusOn = cardDetection,
            tapToFocusEnabled = tapToFocusEnabled,
            focusOnCardEnabled = focusOnCardEnabled,
            showFocusIndicator = showFocusIndicator
        )

        imageSpaceChain?.let { spaceChain ->
            if (isDetectionEnabled && showBoundingBoxes) {
                SimulationDetectionOverlay(
                    cardDetection = cardDetection,
                    imageSpaceChain = spaceChain,
                    showClassNames = showClassNames,
                    classLabels = classLabels
                )
            }

            if (isDetectionEnabled && showLockOnProgress) {
                CardLockOnOverlay(
                    activeDetection = cardDetection,
                    imageSpaceChain = spaceChain
                )
            }
        }

        if (showFlashlightSwitch) {
            IconButton(
                onClick = viewModel::toggleFlashlight,
                modifier = Modifier.padding(16.dp)
            ) {
                Icon(
                    imageVector = if (flashlightEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff,
                    contentDescription = "Toggle Flashlight"
                )
            }
        }

        if (showDebugOverlay) {
            DebugOverlay(
                isDetectionEnabled = isDetectionEnabled,
                useGpu = useGpu,
                showBoundingBoxes = showBoundingBoxes,
                showLockOnProgress = showLockOnProgress,
                imageMode = imageMode,
                inferenceIntervalMs = inferenceIntervalMs,
                tapToFocusEnabled = tapToFocusEnabled,
                focusOnCardEnabled = focusOnCardEnabled,
                lockOnThreshold = lockOnThreshold,
                noDetectionCountLimit = noDetectionCountLimit,
                numThreads = numThreads,
                modifier = Modifier.align(Alignment.BottomStart)
            )
        }
    }
}
