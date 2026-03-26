package com.apexfission.android.carddetectionlite.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.apexfission.android.carddetectionlite.domain.tflite.model.Detection

import kotlin.math.max

@Composable
fun DetectionOverlay(
    detections: List<Detection>,
    scalingInfo: PreviewScalingInfo,
    showClassNames: Boolean,
    classLabels: Map<Int, String>
) {
    val textMeasurer = rememberTextMeasurer()
    val textStyle = TextStyle(
        color = Color.White,
        fontSize = 12.sp,
        background = Color.Black.copy(alpha = 0.5f)
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val screenW = size.width
        val screenH = size.height

        // Calculate how the full camera buffer maps to the screen given FILL_CENTER
        val scale = max(screenW / scalingInfo.fullW, screenH / scalingInfo.fullH)
        val offsetX = (screenW - scalingInfo.fullW * scale) / 2f
        val offsetY = (screenH - scalingInfo.fullH * scale) / 2f

        // The detector results are [0..1] relative to the CROP rectangle.
        // First we map to crop pixels, then we offset by the crop's position in the full buffer.
        // However, since rotateRectToUpright centers the crop, we need to be careful.
        // Assuming the crop covers the whole area used by FILL_CENTER:
        detections.forEach { det ->
            // Scale detection relative to the crop dimensions
            val cropX1 = det.x1Pct * scalingInfo.cropW
            val cropY1 = det.y1Pct * scalingInfo.cropH
            val cropX2 = det.x2Pct * scalingInfo.cropW
            val cropY2 = det.y2Pct * scalingInfo.cropH

            val x1 = cropX1 * scale + offsetX
            val y1 = cropY1 * scale + offsetY
            val x2 = cropX2 * scale + offsetX
            val y2 = cropY2 * scale + offsetY

            drawRect(
                color = Color.Cyan,
                topLeft = Offset(x1, y1),
                size = Size(x2 - x1, y2 - y1),
                style = Stroke(width = 2.dp.toPx())
            )

            if (showClassNames) {
                val label = classLabels[det.classId] ?: "ID: ${det.classId}"


                val displayString = "$label (${(det.confidence * 100).toInt()}%) [${x1}, ${y1}, ${x2}, ${y2}]"
                val textLayout = textMeasurer.measure(displayString, textStyle)
                val textTop = if (y1 - textLayout.size.height < 0) y1 else y1 - textLayout.size.height

                drawText(textLayoutResult = textLayout, topLeft = Offset(x1, textTop))
            }
        }
    }
}