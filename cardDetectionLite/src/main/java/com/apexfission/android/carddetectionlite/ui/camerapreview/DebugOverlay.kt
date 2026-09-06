package com.apexfission.android.carddetectionlite.ui.camerapreview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.apexfission.android.carddetectionlite.domain.tflite.detector.InputShape
import com.apexfission.android.carddetectionlite.ui.NumThreads

/**
 * A debug overlay composable that displays current runtime parameters and configuration states.
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
    imageMode: InputShape,
    inferenceIntervalMs: Long,
    tapToFocusEnabled: Boolean,
    focusOnCardEnabled: Boolean,
    lockOnThreshold: Int,
    numThreads: NumThreads,
    modifier: Modifier = Modifier,
    noDetectionCountLimit: Int,
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
        DebugItem("Threads", numThreads),
        DebugItem("Input", imageMode),
        DebugItem("No Detection Limit", noDetectionCountLimit)
    )

    Column(
        modifier = modifier
            .padding(8.dp)
            .background(Color.Black.copy(alpha = 0.5f))
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
                    is InputShape -> value.name
                    is NumThreads -> value.toString()
                    null -> "Default"
                    else -> value.toString()
                }

                val valueColor = when (val value = item.value) {
                    is Boolean -> if (value) Color(0xFF8BC34A) else Color(0xFFE91E63)
                    is Number -> Color(0xFF2196F3)
                    is InputShape -> when (value) {
                        InputShape.SquareCrop -> Color(0xFFFFC107)
                        InputShape.FullImage -> Color(0xFF9C27B0)
                        InputShape.VisibleImage -> Color(0xFF00BCD4)
                        InputShape.VisibleImageSquareCrop -> Color(0xFFF44336)
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
