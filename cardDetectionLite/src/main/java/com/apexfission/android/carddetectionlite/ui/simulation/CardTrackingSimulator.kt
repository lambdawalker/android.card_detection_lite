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
import com.apexfission.android.carddetectionlite.domain.tflite.detector.InputShape
import com.apexfission.android.carddetectionlite.domain.tflite.filters.AspectRatioValidator
import com.apexfission.android.carddetectionlite.domain.tflite.filters.CardValidator
import com.apexfission.android.carddetectionlite.domain.tflite.filters.MarginValidator
import com.apexfission.android.carddetectionlite.domain.tflite.model.CardDetection2
import com.apexfission.android.carddetectionlite.ui.camerapreview.CardLockOnOverlay
import com.apexfission.android.carddetectionlite.ui.camerapreview.DebugOverlay
import com.apexfission.android.carddetectionlite.ui.NumThreads
import kotlinx.coroutines.flow.MutableStateFlow

@Composable
fun CardTrackingSimulator(
    modifier: Modifier = Modifier,
    videoUri: Uri,
    modelPath: String,
    classLabels: Map<Int, String>,
    cardClasses: Set<Int>,
    isDetectionEnabled: Boolean,
    useGpu: Boolean = false,
    imageMode: InputShape = InputShape.FullImage,
    showBoundingBoxes: Boolean = false,
    showClassNames: Boolean = false,
    showLockOnProgress: Boolean = true,
    showDebugOverlay: Boolean = true,
    scoreThreshold: Float = 0.65f,
    cardFilters: List<CardValidator> = listOf(
        MarginValidator(), AspectRatioValidator()
    ),
    onCardDetection: (CardDetection2) -> Unit,
    inferenceIntervalMs: Long = 33L,
    lockOnThreshold: Int = 5,
    noDetectionCountLimit: Int = 8,
    numThreads: NumThreads = NumThreads.Default
) {
    val context = LocalContext.current
    val sizeInPixels = remember { MutableStateFlow(IntSize.Zero) }

    val viewModel: CardTrackingSimulatorViewModel = viewModel(
        factory = CardTrackingSimulatorViewModelFactory(
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
            if (isDetectionEnabled && showBoundingBoxes) {
                SimulationDetectionOverlay(
                    cardDetection = cardDetection, imageSpaceChain = space, showClassNames = showClassNames, classLabels = classLabels
                )
            }


            if (isDetectionEnabled && showLockOnProgress) {
                CardLockOnOverlay(
                    activeDetection = cardDetection, imageSpaceChain = space
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
                tapToFocusEnabled = false,
                focusOnCardEnabled = false,
                lockOnThreshold = lockOnThreshold,
                noDetectionCountLimit = noDetectionCountLimit,
                numThreads = numThreads,
                modifier = Modifier.align(Alignment.BottomStart)
            )
        }
    }
}

