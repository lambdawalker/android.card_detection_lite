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
import androidx.compose.ui.Alignment
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
import com.apexfission.android.carddetectionlite.ui.camerapreview.CameraPreset
import com.apexfission.android.carddetectionlite.ui.detector.CardDetectorPreset
import com.apexfission.android.carddetectionlite.ui.overlays.OverlayPreset
import com.apexfission.android.carddetectionlite.ui.overlays.CardLockOnOverlay
import com.apexfission.android.carddetectionlite.ui.overlays.DebugOverlay
import com.apexfission.android.carddetectionlite.ui.overlays.DetectionOverlay
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * A simulation composable that runs card tracking inference over video frames from a URI source.
 *
 * Configured via structured preset objects ([CardDetectorPreset], [OverlayPreset], and [CameraPreset]).
 *
 * @param modifier Composable modifier.
 * @param videoUri Source video URI.
 * @param modelPath Asset path to TFLite model.
 * @param classLabels Map of class IDs to human readable labels.
 * @param cardClasses Set of class IDs treated as cards.
 * @param isDetectionEnabled Whether detection is active.
 * @param detectorPreset ML pipeline configuration preset ([CardDetectorPreset]). Defaults to [CardDetectorPreset.HighPerformance].
 * @param overlayPreset UI overlay display configuration preset ([OverlayPreset]). Defaults to [OverlayPreset.Debug].
 * @param cameraPreset CameraX lens and focus configuration preset ([CameraPreset]). Defaults to [CameraPreset.Default].
 * @param cardFilters List of card validators.
 * @param onCardDetection Callback lambda on card detection events.
 */
@Composable
fun CardTrackingSimulator(
    modifier: Modifier = Modifier,
    videoUri: Uri,
    modelPath: String,
    classLabels: Map<Int, String>,
    cardClasses: Set<Int>,
    isDetectionEnabled: Boolean = true,
    detectorPreset: CardDetectorPreset = CardDetectorPreset.HighPerformance,
    overlayPreset: OverlayPreset = OverlayPreset.Debug,
    cameraPreset: CameraPreset = CameraPreset.Default,
    cardFilters: List<CardValidator> = listOf(
        MarginValidator(), AspectRatioValidator()
    ),
    onCardDetection: (CardDetection) -> Unit,
) {
    val context = LocalContext.current
    val sizeInPixels = remember { MutableStateFlow(IntSize.Zero) }

    val viewModel: CardTrackingSimulatorViewModel = viewModel(
        factory = CardTrackingSimulatorViewModelFactory(
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
            numThreads = detectorPreset.numThreads,
        )
    )

    LaunchedEffect(isDetectionEnabled) {
        viewModel.setDetectionEnabled(isDetectionEnabled)
    }

    val cardDetection by viewModel.cardDetection.collectAsStateWithLifecycle()
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

        imageSpaceChain?.let { space ->
            if (isDetectionEnabled && overlayPreset.showBoundingBoxes) {
                DetectionOverlay(
                    cardDetection = cardDetection, imageSpaceChain = space, showClassNames = overlayPreset.showClassNames, classLabels = classLabels
                )
            }

            if (isDetectionEnabled && overlayPreset.showLockOnProgress) {
                CardLockOnOverlay(
                    activeDetection = cardDetection, imageSpaceChain = space
                )
            }
        }

        if (overlayPreset.showDebugOverlay) {
            DebugOverlay(
                isDetectionEnabled = isDetectionEnabled,
                useGpu = detectorPreset.useGpu,
                showBoundingBoxes = overlayPreset.showBoundingBoxes,
                showLockOnProgress = overlayPreset.showLockOnProgress,
                imageMode = detectorPreset.imageMode,
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
