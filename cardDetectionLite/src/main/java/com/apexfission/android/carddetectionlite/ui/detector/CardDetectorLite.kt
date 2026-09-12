package com.apexfission.android.carddetectionlite.ui.detector

import android.app.Application
import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

/**
 * All-in-one Composable that provides a configurable in-camera-feed card detection and tracking solution.
 *
 * Configured via structured preset objects ([CardDetectorPreset] and [CameraPreset]),
 * and extensible via a developer-customizable scoped slot API ([controlOverlay]).
 *
 * @param modifier A [Modifier] applied to the root `Box` of this component.
 * @param instanceKey Stable identity for this detector within its [androidx.lifecycle.ViewModelStoreOwner].
 * Use a different value for sibling detector components. Changing detector configuration recreates
 * the detector ViewModel; camera-only configuration and callbacks do not.
 * @param modelPath The path to the `.tflite` model file within the application's `assets` directory.
 * @param classLabels A map where keys are integer class IDs and values are human-readable string labels.
 * @param cardClasses A set of class IDs from the model that should be treated as primary targets for card detection.
 * @param isDetectionEnabled A boolean flag to dynamically start or stop the detection process.
 * @param detectorPreset ML pipeline configuration preset ([CardDetectorPreset]). Defaults to [CardDetectorPreset.HighPerformance].
 * @param cameraPreset CameraX lens and focus configuration preset ([CameraPreset]). Defaults to [CameraPreset.Default].
 * @param cardFilters A list of [CardValidator] instances used to apply additional heuristic validation rules.
 * @param onCardDetection Invoked on a library worker thread with a bitmap owned by the application.
 * The application must recycle the bitmap after its final use.
 * @param onBack An optional callback lambda invoked when the user taps the back button in the overlay.
 * @param onCapture Invoked on a library worker thread with the retained best detection and an
 * independent bitmap copy. The application owns and must recycle the bitmap after its final use.
 * @param controlOverlay A scoped Compose slot allowing developers to provide a custom overlay UI via [CardDetectorOverlayScope].
 */
@Composable
fun CardDetectorLite(
    modifier: Modifier = Modifier,
    instanceKey: String,
    modelPath: String,
    classLabels: Map<Int, String>,
    cardClasses: Set<Int>,
    isDetectionEnabled: Boolean = true,
    detectorPreset: CardDetectorPreset = CardDetectorPreset.HighPerformance,
    cameraPreset: CameraPreset = CameraPreset.HighResolution,
    cardFilters: List<CardValidator> = listOf(
        MarginValidator(), AspectRatioValidator()
    ),
    onCardDetection: (CardDetection, Bitmap) -> Unit = { _, _ -> },
    onBack: () -> Unit = {},
    onCapture: (CardDetection, Bitmap) -> Unit = { _, _ -> },
    controlOverlay: @Composable CardDetectorOverlayScope.() -> Unit = {}
) {
    val context = LocalContext.current
    val captureScope = rememberCoroutineScope()
    val imageSpaceChainFlow = remember { MutableStateFlow<ImageSpaceChain?>(null) }
    val detectorViewModelKey = detectorViewModelKey(
        component = DetectorComponent.CAMERA,
        instanceKey = instanceKey,
        modelPath = modelPath,
        classLabels = classLabels,
        cardClasses = cardClasses,
        detectorPreset = detectorPreset,
        cardFilters = cardFilters,
    )

    val viewModel: CardDetectorLiteViewModel = viewModel(
        key = detectorViewModelKey,
        factory = CardDetectorLiteViewModelFactory(
            application = context.applicationContext as Application,
            modelPath = modelPath,
            classLabels = classLabels,
            cardClasses = cardClasses,
            useGpu = detectorPreset.useGpu,
            scoreThreshold = detectorPreset.scoreThreshold,
            iouThreshold = detectorPreset.iouThreshold,
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

    val sizeInPixels = remember { MutableStateFlow(IntSize.Zero) }
    val viewportSize by sizeInPixels.collectAsStateWithLifecycle()
    val flashlightEnabled by viewModel.flashlightEnabled.collectAsStateWithLifecycle()
    val flashlightAvailable by viewModel.flashlightAvailable.collectAsStateWithLifecycle()
    val detectionFrame by viewModel.detectionFrame.collectAsStateWithLifecycle()
    val cardDetection = detectionFrame.detection
    val latestBestDetection by viewModel.latestBestDetection.collectAsStateWithLifecycle()
    val imageSpaceChain by imageSpaceChainFlow.collectAsStateWithLifecycle()

    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { size ->
                sizeInPixels.value = size
            }
    ) {

        CameraPreview(
            lifecycleOwner = LocalLifecycleOwner.current,
            onFrame = { imageProxy, spaceChain ->
                imageSpaceChainFlow.value = spaceChain
                viewModel.processImage(
                    imageProxy = imageProxy,
                    canvasSize = viewportSize,
                    onDetection = onCardDetection
                )
            },
            onFocusEvent = viewModel::onFocusEvent,
            flashlightEnabled = flashlightEnabled,
            onFlashlightAvailabilityChanged = viewModel::setFlashlightAvailable,
            analysisTargetResolution = cameraPreset.analysisTargetResolution,
            focusOn = cardDetection,
            focusImageSpaceChain = imageSpaceChain,
            tapToFocusEnabled = cameraPreset.tapToFocusEnabled,
            focusOnCardEnabled = cameraPreset.focusOnCardEnabled,
            showFocusIndicator = true
        )

        val overlayScope = remember(
            cardDetection,
            detectionFrame.sequence,
            latestBestDetection,
            imageSpaceChain,
            flashlightAvailable,
            flashlightEnabled,
            detectorPreset,
            cameraPreset,
            classLabels,
            onCapture,
            onBack,
            onCardDetection
        ) {
            CardDetectorOverlayScopeImpl(
                detectionState = cardDetection,
                detectionSequence = detectionFrame.sequence,
                latestBestDetection = latestBestDetection,
                imageSpaceChain = imageSpaceChain,
                flashlightAvailable = flashlightAvailable,
                flashlightEnabled = flashlightEnabled,
                detectorPreset = detectorPreset,
                cameraPreset = cameraPreset,
                classLabels = classLabels,
                onCaptureRequested = {
                    captureScope.launch {
                        viewModel.captureLatest(onCapture)
                    }
                },
                onBackRequested = onBack,
                onFlashlightToggleRequested = viewModel::toggleFlashlight
            )
        }

        overlayScope.controlOverlay()
    }
}
