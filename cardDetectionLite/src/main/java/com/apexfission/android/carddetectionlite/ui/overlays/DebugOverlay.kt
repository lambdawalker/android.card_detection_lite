package com.apexfission.android.carddetectionlite.ui.overlays

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.apexfission.android.carddetectionlite.domain.tflite.detector.PreProcessingImageTransformation
import com.apexfission.android.carddetectionlite.ui.detector.CardDetectorOverlayScope
import com.apexfission.android.carddetectionlite.ui.detector.NumThreads

/**
 * Scoped variant of [DebugOverlay] using [CardDetectorOverlayScope].
 *
 * Automatically pulls runtime parameters from [CardDetectorOverlayScope.detectorPreset]
 * and [CardDetectorOverlayScope.cameraPreset].
 *
 * @param modifier Composable modifier.
 * @param isDetectionEnabled Whether card detection is active.
 * @param showBoundingBoxes Whether bounding boxes are displayed.
 * @param showLockOnProgress Whether lock-on progress is enabled.
 */
@Composable
fun CardDetectorOverlayScope.DebugOverlay(
    modifier: Modifier = Modifier,
    isDetectionEnabled: Boolean = true,
    showBoundingBoxes: Boolean = false,
    showLockOnProgress: Boolean = true
) {
    DebugOverlay(
        isDetectionEnabled = isDetectionEnabled,
        useGpu = detectorPreset.useGpu,
        showBoundingBoxes = showBoundingBoxes,
        showLockOnProgress = showLockOnProgress,
        imageMode = detectorPreset.preProcessingImageTransformation,
        inferenceIntervalMs = detectorPreset.inferenceIntervalMs,
        tapToFocusEnabled = cameraPreset.tapToFocusEnabled,
        focusOnCardEnabled = cameraPreset.focusOnCardEnabled,
        lockOnThreshold = detectorPreset.lockOnThreshold,
        numThreads = detectorPreset.numThreads,
        noDetectionCountLimit = detectorPreset.noDetectionCountLimit,
        memoryDetectionTimeLimit = detectorPreset.memoryDetectionTimeLimit,
        validateClassIdInLockOnProcess = detectorPreset.validateClassIdInLockOnProcess,
        differenceHashDistanceLimit = detectorPreset.differenceHashDistanceLimit,
        allowTemporalDrift = detectorPreset.allowTemporalDrift,
        modifier = modifier
    )
}

/**
 * Standalone variant of [DebugOverlay] taking explicit parameters.
 *
 * Positioned at the bottom of the screen with navigation bar padding.
 *
 * @param isDetectionEnabled Whether card detection is active.
 * @param useGpu Whether GPU inference is enabled.
 * @param showBoundingBoxes Whether bounding boxes are shown.
 * @param showLockOnProgress Whether lock-on progress is enabled.
 * @param imageMode Current input shape configuration.
 * @param inferenceIntervalMs Minimum time between inferences in milliseconds.
 * @param tapToFocusEnabled Whether tap-to-focus is enabled.
 * @param focusOnCardEnabled Whether smart auto-focus on card is enabled.
 * @param lockOnThreshold Number of consistent frames required for lock-on.
 * @param numThreads CPU thread configuration.
 * @param modifier Composable modifier.
 * @param noDetectionCountLimit Missing detection limit before tracking resets.
 */
@Composable
fun DebugOverlay(
    isDetectionEnabled: Boolean,
    useGpu: Boolean,
    showBoundingBoxes: Boolean,
    showLockOnProgress: Boolean,
    imageMode: PreProcessingImageTransformation,
    inferenceIntervalMs: Long,
    tapToFocusEnabled: Boolean,
    focusOnCardEnabled: Boolean,
    lockOnThreshold: Int,
    numThreads: NumThreads,
    modifier: Modifier = Modifier,
    noDetectionCountLimit: Int = 8,
    memoryDetectionTimeLimit: Long = 1000L,
    validateClassIdInLockOnProcess: Boolean = true,
    differenceHashDistanceLimit: Int = 25,
    allowTemporalDrift: Boolean = true,
) {
    data class DebugItem(val name: String, val value: Any?)

    val debugItems = listOf(
        DebugItem("Detection Enabled", isDetectionEnabled),
        DebugItem("GPU", useGpu),
        DebugItem("BBoxes", showBoundingBoxes),
        DebugItem("Show LockOn", showLockOnProgress),
        DebugItem("Tap to focus", tapToFocusEnabled),
        DebugItem("Card to focus", focusOnCardEnabled),
        DebugItem("Inference interval", inferenceIntervalMs),
        DebugItem("Lock on threshold", lockOnThreshold),
        DebugItem("No Detection Limit", noDetectionCountLimit),
        DebugItem("Memory Time Limit", memoryDetectionTimeLimit),
        DebugItem("Validate Class ID", validateClassIdInLockOnProcess),
        DebugItem("dHash Limit", differenceHashDistanceLimit),
        DebugItem("Temporal Drift", allowTemporalDrift),
        DebugItem("Threads", numThreads),
        DebugItem("Input", imageMode)
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(8.dp)
                .background(Color.Black.copy(alpha = 0.75f))
                .padding(8.dp)
        ) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(debugItems) { item ->
                    val valueString = when (val value = item.value) {
                        is Long -> "${value}ms"
                        is PreProcessingImageTransformation -> when (value) {
                            is PreProcessingImageTransformation.FullImage -> "FullImage"
                            is PreProcessingImageTransformation.CenterSquareCrop -> "CenterSquareCrop"
                            is PreProcessingImageTransformation.SquareCrop -> "SquareCrop(top=${value.top})"
                            is PreProcessingImageTransformation.CenterVisibleImage -> "CenterVisibleImage"
                            is PreProcessingImageTransformation.VisibleImage -> "VisibleImage(top=${value.top})"
                            is PreProcessingImageTransformation.CenterVisibleImageSquareCrop -> "CenterVisibleImageSquareCrop"
                            is PreProcessingImageTransformation.VisibleImageSquareCrop -> "VisibleImageSquareCrop(top=${value.top})"
                        }
                        is NumThreads -> value.toString()
                        null -> "Default"
                        else -> value.toString()
                    }

                    val valueColor = when (val value = item.value) {
                        is Boolean -> if (value) Color(0xFF8BC34A) else Color(0xFFE91E63)
                        is Number -> Color(0xFF2196F3)
                        is PreProcessingImageTransformation -> when (value) {
                            is PreProcessingImageTransformation.SquareCrop, is PreProcessingImageTransformation.CenterSquareCrop -> Color(0xFFFFC107)
                            is PreProcessingImageTransformation.FullImage -> Color(0xFF9C27B0)
                            is PreProcessingImageTransformation.VisibleImage, is PreProcessingImageTransformation.CenterVisibleImage -> Color(0xFF00BCD4)
                            is PreProcessingImageTransformation.VisibleImageSquareCrop, is PreProcessingImageTransformation.CenterVisibleImageSquareCrop -> Color(0xFFF44336)
                        }

                        is NumThreads -> Color.White
                        else -> Color.White
                    }

                    Text(
                        fontSize = 10.sp,
                        text = buildAnnotatedString {
                            withStyle(style = SpanStyle(color = Color.White)) {
                                append("${item.name}: ")
                            }
                            withStyle(style = SpanStyle(color = valueColor)) {
                                append(valueString)
                            }
                        }
                    )
                }
            }
        }
    }
}
