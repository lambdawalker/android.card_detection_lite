package com.apexfission.android.carddetectionlite.ui

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
    modifier: Modifier = Modifier
) {
    data class DebugItem(val name: String, val value: Any?)

    val debugItems = listOf(
        DebugItem("Detection", isDetectionEnabled),
        DebugItem("GPU", useGpu),
        DebugItem("BBoxes", showBoundingBoxes),
        DebugItem("LockOn", showLockOnProgress),
        DebugItem("TapFocus", tapToFocusEnabled),
        DebugItem("CardFocus", focusOnCardEnabled),
        DebugItem("Interval", inferenceIntervalMs),
        DebugItem("LockThresh", lockOnThreshold),
        DebugItem("Threads", numThreads),
        DebugItem("Input", imageMode)
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
                    is Boolean -> if (value) Color(0xFF8BC34A) else Color(0xFFE91E63) // Green / Pink
                    is Number -> Color(0xFF2196F3) // Blue
                    is InputShape -> when (value) {
                        InputShape.SquareCrop -> Color(0xFFFFC107) // Amber
                        InputShape.FullImage -> Color(0xFF9C27B0) // Purple
                        InputShape.VisibleImage -> Color(0xFF00BCD4) // Cyan
                        InputShape.VisibleImageSquareCrop -> Color(0xFFF44336) // Red
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
