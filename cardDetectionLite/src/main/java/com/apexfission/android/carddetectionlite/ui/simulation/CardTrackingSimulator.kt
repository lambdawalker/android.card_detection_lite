package com.apexfission.android.carddetectionlite.ui.simulation

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.apexfission.android.carddetectionlite.domain.coordinates.models.ImageSpaceChain
import com.apexfission.android.carddetectionlite.domain.tflite.filters.AspectRatioValidator
import com.apexfission.android.carddetectionlite.domain.tflite.filters.CardValidator
import com.apexfission.android.carddetectionlite.domain.tflite.filters.MarginValidator
import com.apexfission.android.carddetectionlite.domain.tflite.model.CardDetection
import com.apexfission.android.carddetectionlite.resource.BitmapTransfer
import com.apexfission.android.carddetectionlite.ui.camerapreview.CameraPreset
import com.apexfission.android.carddetectionlite.ui.detector.CardDetectorOverlayScope
import com.apexfission.android.carddetectionlite.ui.detector.CardDetectorOverlayScopeImpl
import com.apexfission.android.carddetectionlite.ui.detector.CardDetectorPreset
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * A simulation composable that runs card tracking inference over video frames from a URI source.
 *
 * Configured via structured preset objects ([CardDetectorPreset] and [CameraPreset]).
 *
 * @param modifier Composable modifier.
 * @param instanceKey Stable identity for this simulator within its [androidx.lifecycle.ViewModelStoreOwner].
 * Use a different value for sibling detector components. Changing detector configuration recreates
 * the detector ViewModel; video, camera-only configuration, and callbacks do not.
 * @param videoUri Source video URI.
 * @param modelPath Asset path to TFLite model.
 * @param classLabels Map of class IDs to human readable labels.
 * @param cardClasses Set of class IDs treated as cards.
 * @param isDetectionEnabled Whether detection is active.
 * @param detectorPreset ML pipeline configuration preset ([CardDetectorPreset]). Defaults to [CardDetectorPreset.HighPerformance].
 * @param cameraPreset CameraX lens and focus configuration preset ([CameraPreset]). Defaults to [CameraPreset.Default].
 * @param cardFilters List of card validators.
 * @param onCardDetection Invoked with detection metadata and a callback-scoped bitmap transfer.
 * @param controlOverlay A scoped Compose slot allowing custom overlays via [CardDetectorOverlayScope].
 */
@Composable
fun CardTrackingSimulator(
    modifier: Modifier = Modifier,
    instanceKey: String,
    videoUri: Uri,
    modelPath: String,
    classLabels: Map<Int, String>,
    cardClasses: Set<Int>,
    isDetectionEnabled: Boolean = true,
    detectorPreset: CardDetectorPreset = CardDetectorPreset.HighPerformance,
    cameraPreset: CameraPreset = CameraPreset.Default,
    cardFilters: List<CardValidator> = listOf(
        MarginValidator(), AspectRatioValidator()
    ),
    onCardDetection: (CardDetection, BitmapTransfer) -> Unit,
    onCaptureRequested: (CardDetection, BitmapTransfer) -> Unit = { _, _ -> },
    onBackRequested: () -> Unit = {},

    controlOverlay: @Composable CardDetectorOverlayScope.() -> Unit = {}
) {
    val context = LocalContext.current
    val sizeInPixels = remember { MutableStateFlow(IntSize.Zero) }
    val detectorViewModelKey = detectorViewModelKey(
        component = DetectorComponent.SIMULATOR,
        instanceKey = instanceKey,
        modelPath = modelPath,
        classLabels = classLabels,
        cardClasses = cardClasses,
        detectorPreset = detectorPreset,
        cardFilters = cardFilters,
    )

    val viewModel: CardTrackingSimulatorViewModel = viewModel(
        key = detectorViewModelKey,
        factory = CardTrackingSimulatorViewModelFactory(
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

    val cardDetection by viewModel.cardDetection.collectAsStateWithLifecycle()
    val latestBestDetection by viewModel.latestBestDetection.collectAsStateWithLifecycle()
    val imageSpaceChainFlow = remember { MutableStateFlow<ImageSpaceChain?>(null) }

    Box(
        modifier
            .fillMaxSize()
            .onSizeChanged { size ->
                sizeInPixels.value = size
            }) {

        VideoPreviewWithFullFrameCapture(
            videoUri = videoUri,
            modifier = Modifier.fillMaxSize(),
            onFrame = { bitmap, imageSpaceChain ->
                imageSpaceChainFlow.value = imageSpaceChain
                Log.d("onFrame", "${bitmap.width} x ${bitmap.height}")
                viewModel.processBitmap(
                    bitmap, onCardDetection
                )
            },
        )

        val imageSpaceChain by imageSpaceChainFlow.collectAsStateWithLifecycle()



        val overlayScope = remember(
            cardDetection,
            latestBestDetection,
            imageSpaceChain,
            detectorPreset,
            cameraPreset,
            classLabels,
            onCardDetection,
            onCaptureRequested
        ) {
            CardDetectorOverlayScopeImpl(
                detectionState = cardDetection,
                latestBestDetection = latestBestDetection,
                imageSpaceChain = imageSpaceChain,
                flashlightAvailable = false,
                flashlightEnabled = false,
                detectorPreset = detectorPreset,
                cameraPreset = cameraPreset,
                classLabels = classLabels,
                onCaptureRequested = {
                    viewModel.captureLatest(onCaptureRequested)
                },
                onBackRequested = onBackRequested,
                onFlashlightToggleRequested = {}
            )
        }

        overlayScope.controlOverlay()
    }
}
