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


/**
 * All-in-one Composable that provides a configurable in camera feed card detection solution,
 * segmentation step before OCR.
 *
 * This component seamlessly integrates the `CameraPreview`, `DetectionOverlay`, and `CardLockOnOverlay`
 * with the underlying `CardDetectorLiteViewModel`. It serves as the primary entry point for using
 * the card detection feature in an application.
 *
 * @param modelPath The path to the `.tflite` model file within the application's `assets` directory.
 * @param classLabels A map where keys are the integer class IDs from the model and values are the
 *                    corresponding human-readable string labels (e.g., `0 to "credit_card"`).
 * @param cardClasses A specific list of class IDs from the model that should be treated as the primary
 *                    target for detection and lock-on.
 * @param isDetectionEnabled A boolean flag to dynamically start or stop the detection process. When `false`,
 *                           the camera preview remains active, but no model inference is performed.
 * @param modifier A [Modifier] applied to the root `Box` of this component.
 * @param useGpu If `true`, the underlying TFLite interpreter will attempt to use the GPU delegate
 *               for potentially faster inference.
 * @param showBoundingBoxes When `true`, a [DetectionOverlay] is displayed, drawing boxes around all
 *                          detected objects (both the card and its sub-features).
 * @param showClassNames If `true` (and `showBoundingBoxes` is also true), labels with the class name
 *                       and confidence score will be drawn above each bounding box.
 * @param showFlashlightSwitch If `true`, a UI button is provided to allow the user to toggle the
 *                             camera's flashlight.
 * @param showLockOnProgress If `true`, the [CardLockOnOverlay] is displayed, providing visual
 *                           feedback as the detector locks onto a card.
 * @param scoreThreshold The minimum confidence score (0.0 to 1.0) a detection must have to be considered.
 *                       Lowering this may increase recall but can also lead to more false positives.
 * @param analysisTargetResolution The target resolution for the image analysis stream passed to `CameraPreview`.
 * @param cardCardFilters A list of [CardValidator] instances used to apply additional heuristic
 *                        checks on potential card detections (e.g., ensuring a plausible aspect ratio).
 * @param onCardDetection A callback lambda that is invoked only when the detector achieves a stable
 *                        "lock-on" on a card (`lockOnProgress >= 1.0`). It provides the final,
 *                        validated [CardDetection] object.
 * @param imageMode The [InputShape] that dictates how the camera image is preprocessed (e.g., cropped)
 *                  before being sent to the model.
 * @param inferenceIntervalMs The minimum interval, in milliseconds, between consecutive inferences.
 * @param tapToFocusEnabled A boolean flag to enable or disable the tap-to-focus feature.
 */
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
    imageMode: InputShape = InputShape.SquareCrop,
    inferenceIntervalMs: Long = 33L,
    tapToFocusEnabled: Boolean = true,
) {
    val context = LocalContext.current
    val sizeInPixels = MutableStateFlow(IntSize.Zero)

    // Instantiate the ViewModel using the factory to pass in all necessary parameters.
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
            inferenceIntervalMs = inferenceIntervalMs,
        )
    )

    // Control the ViewModel's detection state.
    LaunchedEffect(isDetectionEnabled) {
        viewModel.setDetectionEnabled(isDetectionEnabled)
    }

    // Collect state from the ViewModel to drive the UI.
    val flashlightEnabled by viewModel.flashlightEnabled.collectAsStateWithLifecycle()
    val cardDetection by viewModel.cardDetection.collectAsStateWithLifecycle()
    val scalingInfo by viewModel.scalingInfo.collectAsStateWithLifecycle()

    Box(
        modifier
            .fillMaxSize()
            .onSizeChanged { size ->
                // Keep the ViewModel aware of the Composable's size for aspect ratio calculations.
                sizeInPixels.value = size
            }) {

        CameraPreview(
            lifecycleOwner = LocalLifecycleOwner.current,
            onFrame = { imageProxy -> viewModel.processImage(imageProxy, onCardDetection) },
            onFocusEvent = viewModel::onFocusEvent,
            flashlightEnabled = flashlightEnabled,
            analysisTargetResolution = analysisTargetResolution,
            focusOn = cardDetection,
            tapToFocusEnabled = tapToFocusEnabled
        )

        // Conditionally display overlays based on configuration and state.
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
                onClick = viewModel::toggleFlashlight,
                modifier = Modifier.padding(16.dp)
            ) {
                Icon(
                    imageVector = if (flashlightEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff,
                    contentDescription = "Toggle Flashlight"
                )
            }
        }
    }
}
