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
import com.apexfission.android.carddetectionlite.domain.tflite.model.CardDetection
import kotlin.math.max

/**
 * A Composable that renders bounding boxes and labels for detected objects onto a `Canvas`.
 *
 * This overlay is designed to be drawn on top of the `CameraPreview`. Its primary responsibility
 * is to solve the complex coordinate transformation problem: converting the normalized,
 * model-space coordinates of a detection into the correct pixel-space coordinates on the screen.
 * It accounts for differences in aspect ratio and scaling between the camera's raw output and
 * the `PreviewView`'s `FILL_CENTER` display mode.
 *
 * @param cardDetection The [CardDetection] result from the ViewModel. This object contains the list
 *                      of features (the card and any other objects) to be drawn. If `null`,
 *                      nothing is drawn.
 * @param scalingInfo A [PreviewScalingInfo] object, also from the ViewModel. This is the critical
 *                    piece of metadata containing the dimensions of the camera buffer and the
 *                    cropped analysis region, which is necessary to perform the coordinate mapping.
 * @param showClassNames A boolean flag. If `true`, a text label with the object's class name and
 *                       confidence score is drawn above each bounding box.
 * @param classLabels A map that translates integer class IDs from the model into human-readable
 *                    string labels for display.
 */
@Composable
fun DetectionOverlay(
    cardDetection: CardDetection?,
    scalingInfo: PreviewScalingInfo,
    showClassNames: Boolean,
    classLabels: Map<Int, String>
) {
    val textMeasurer = rememberTextMeasurer()
    val textStyle = TextStyle(color = Color.White, fontSize = 12.sp, background = Color.Black.copy(alpha = 0.5f))

    Canvas(modifier = Modifier.fillMaxSize()) {
        val screenW = size.width
        val screenH = size.height

        // --- Coordinate Transformation Logic ---
        // The camera buffer (fullW x fullH) is scaled to fit the screen (screenW x screenH)
        // using a "fill center" strategy. We need to replicate that scaling logic here.
        val scale = max(screenW / scalingInfo.fullW, screenH / scalingInfo.fullH)
        val offsetX = (screenW - scalingInfo.fullW * scale) / 2f
        val offsetY = (screenH - scalingInfo.fullH * scale) / 2f

        val features = cardDetection?.let { it.features + it.card } ?: emptyList()

        features.forEach { feature ->
            val det = feature.detection

            // Step 1: The detection's coordinates (`x1Pct`, etc.) are normalized relative to the
            // 'context' (the potentially cropped image fed to the model). First, we convert
            // these percentages into pixel coordinates within that context.
            val cropX1 = det.contextX1Pct * scalingInfo.cropW
            val cropY1 = det.contextY1Pct * scalingInfo.cropH
            val cropX2 = det.contextX2Pct * scalingInfo.cropW
            val cropY2 = det.contextY2Pct * scalingInfo.cropH

            // Step 2: Now, we map these context-space pixel coordinates to the final screen space.
            // We apply the same scaling factor and offset that the PreviewView uses to display
            // the camera feed.
            val x1 = cropX1 * scale + offsetX
            val y1 = cropY1 * scale + offsetY
            val x2 = cropX2 * scale + offsetX
            val y2 = cropY2 * scale + offsetY

            // Draw the final, correctly positioned bounding box.
            drawRect(
                color = Color.Cyan,
                topLeft = Offset(x1, y1),
                size = Size(x2 - x1, y2 - y1),
                style = Stroke(width = 2.dp.toPx())
            )

            // Optionally, draw the text label.
            if (showClassNames) {
                val label = classLabels[det.classId] ?: "ID: ${det.classId}"
                val displayString = "$label (${(det.confidence * 100).toInt()}%)"
                val textLayout = textMeasurer.measure(displayString, textStyle)

                // Position the text above the box, but clamp it to the top of the screen.
                val textTop = (y1 - textLayout.size.height).coerceAtLeast(0f)

                drawText(textLayoutResult = textLayout, topLeft = Offset(x1, textTop))
            }
        }
    }
}
