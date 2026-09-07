package com.apexfission.android.carddetectionlite.ui.detector

import android.app.Application
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
import com.apexfission.android.carddetectionlite.domain.tflite.filters.AspectRatioValidator
import com.apexfission.android.carddetectionlite.domain.tflite.filters.CardValidator
import com.apexfission.android.carddetectionlite.domain.tflite.filters.MarginValidator
import com.apexfission.android.carddetectionlite.domain.tflite.model.CardDetection
import com.apexfission.android.carddetectionlite.ui.camerapreview.CameraPreset
import com.apexfission.android.carddetectionlite.ui.camerapreview.CameraPreview
import com.apexfission.android.carddetectionlite.ui.overlays.AreaOfInterest
import com.apexfission.android.carddetectionlite.ui.overlays.CardLockOnOverlay
import com.apexfission.android.carddetectionlite.ui.overlays.DebugOverlay
import com.apexfission.android.carddetectionlite.ui.overlays.DetectionOverlay
import com.apexfission.android.carddetectionlite.ui.overlays.OverlayPreset
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * All-in-one Composable that provides a configurable in-camera-feed card detection and tracking solution.
 *
 * Configured via structured preset objects ([CardDetectorPreset], [OverlayPreset], and [CameraPreset]).
 *
 * @param modifier A [Modifier] applied to the root `Box` of this component.
 * @param modelPath The path to the `.tflite` model file within the application's `assets` directory.
 * @param classLabels A map where keys are integer class IDs and values are human-readable string labels.
 * @param cardClasses A set of class IDs from the model that should be treated as primary targets for card detection.
 * @param isDetectionEnabled A boolean flag to dynamically start or stop the detection process.
 * @param detectorPreset ML pipeline configuration preset ([CardDetectorPreset]). Defaults to [CardDetectorPreset.HighPerformance].
 * @param overlayPreset UI overlay display configuration preset ([OverlayPreset]). Defaults to [OverlayPreset.Standard].
 * @param cameraPreset CameraX lens and focus configuration preset ([CameraPreset]). Defaults to [CameraPreset.Default].
 * @param cardFilters A list of [CardValidator] instances used to apply additional heuristic validation rules.
 * @param onCardDetection A callback lambda invoked when a card detection event occurs.
 */
@Composable
fun CardDetectorLite(
    modifier: Modifier = Modifier,
    modelPath: String,
    classLabels: Map<Int, String>,
    cardClasses: Set<Int>,
    isDetectionEnabled: Boolean = true,
    detectorPreset: CardDetectorPreset = CardDetectorPreset.HighPerformance,
    overlayPreset: OverlayPreset = OverlayPreset.Standard,
    cameraPreset: CameraPreset = CameraPreset.Default,
    cardFilters: List<CardValidator> = listOf(
        MarginValidator(), AspectRatioValidator()
    ),
    onCardDetection: (CardDetection) -> Unit,
) {
    val context = LocalContext.current
    val imageSpaceChainFlow = remember { MutableStateFlow<ImageSpaceChain?>(null) }

    val viewModel: CardDetectorLiteViewModel = viewModel(
        factory = CardDetectorLiteViewModelFactory(
            application = context.applicationContext as Application,
            modelPath = modelPath,
            cardClasses = cardClasses,
            useGpu = detectorPreset.useGpu,
            scoreThreshold = detectorPreset.scoreThreshold,
            cardFilters = cardFilters,
            inferenceIntervalMs = detectorPreset.inferenceIntervalMs,
            lockOnThreshold = detectorPreset.lockOnThreshold,
            noDetectionCountLimit = detectorPreset.noDetectionCountLimit,
            memoryDetectionTimeLimit = detectorPreset.memoryDetectionTimeLimit,
            validateClassIdInLockOnProcess = detectorPreset.validateClassIdInLockOnProcess,
            differenceHashDistanceLimit = detectorPreset.differenceHashDistanceLimit,
            allowTemporalDrift = detectorPreset.allowTemporalDrift,
            preProcessingImageTransformation = detectorPreset.preProcessingImageTransformation,
            numThreads = detectorPreset.numThreads,
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
            analysisTargetResolution = cameraPreset.analysisTargetResolution,
            focusOn = cardDetection,
            tapToFocusEnabled = cameraPreset.tapToFocusEnabled,
            focusOnCardEnabled = cameraPreset.focusOnCardEnabled,
            showFocusIndicator = overlayPreset.showFocusIndicator
        )

        imageSpaceChain?.let { spaceChain ->
            if (overlayPreset.showAreaOfInterest) {
                AreaOfInterest(
                    preProcessingImageTransformation = detectorPreset.preProcessingImageTransformation,
                    imageSpaceChain = spaceChain
                )
            }

            if (isDetectionEnabled && overlayPreset.showBoundingBoxes) {
                DetectionOverlay(
                    cardDetection = cardDetection,
                    imageSpaceChain = spaceChain,
                    showClassNames = overlayPreset.showClassNames,
                    classLabels = classLabels
                )
            }

            if (isDetectionEnabled && overlayPreset.showLockOnProgress) {
                CardLockOnOverlay(
                    activeDetection = cardDetection,
                    imageSpaceChain = spaceChain
                )
            }
        }

        if (overlayPreset.showFlashlightSwitch) {
            IconButton(
                onClick = viewModel::toggleFlashlight,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
            ) {
                Icon(
                    imageVector = if (flashlightEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff,
                    contentDescription = "Toggle Flashlight"
                )
            }
        }

        if (overlayPreset.showDebugOverlay) {
            DebugOverlay(
                isDetectionEnabled = isDetectionEnabled,
                useGpu = detectorPreset.useGpu,
                showBoundingBoxes = overlayPreset.showBoundingBoxes,
                showLockOnProgress = overlayPreset.showLockOnProgress,
                imageMode = detectorPreset.preProcessingImageTransformation,
                inferenceIntervalMs = detectorPreset.inferenceIntervalMs,
                tapToFocusEnabled = cameraPreset.tapToFocusEnabled,
                focusOnCardEnabled = cameraPreset.focusOnCardEnabled,
                lockOnThreshold = detectorPreset.lockOnThreshold,
                noDetectionCountLimit = detectorPreset.noDetectionCountLimit,
                memoryDetectionTimeLimit = detectorPreset.memoryDetectionTimeLimit,
                validateClassIdInLockOnProcess = detectorPreset.validateClassIdInLockOnProcess,
                differenceHashDistanceLimit = detectorPreset.differenceHashDistanceLimit,
                allowTemporalDrift = detectorPreset.allowTemporalDrift,
                numThreads = detectorPreset.numThreads,
                modifier = Modifier.align(Alignment.BottomStart)
            )
        }
    }
}
