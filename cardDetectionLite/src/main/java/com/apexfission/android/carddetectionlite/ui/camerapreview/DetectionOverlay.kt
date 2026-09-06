package com.apexfission.android.carddetectionlite.ui.camerapreview

import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import com.apexfission.android.carddetectionlite.domain.coordinates.models.ImageSpaceChain
import com.apexfission.android.carddetectionlite.domain.coordinates.transformations.toChildSpace
import com.apexfission.android.carddetectionlite.domain.tflite.model.CardDetection
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * A Composable that renders bounding boxes and labels for detected objects onto a `Canvas`.
 *
 * This overlay is designed to be drawn on top of the `CameraPreview`. Its primary responsibility
 * is to solve the complex coordinate transformation problem: converting the normalized,
 * model-space coordinates of a detection into the correct pixel-space coordinates on the screen.
 * It accounts for differences in aspect ratio and scaling between the camera's raw output and
 * the `PreviewView`'s `FILL_CENTER` display mode.
 *
 * @param cardDetection The [CardDetection] result from the ViewModel.
 * @param showClassNames A boolean flag. If `true`, a text label with the object's class name and
 *                       confidence score is drawn above each bounding box.
 * @param classLabels A map that translates integer class IDs from the model into human-readable
 *                    string labels for display.
 */
@Composable
fun DetectionOverlay(
    cardDetection: CardDetection?, imageSpaceChain: ImageSpaceChain, showClassNames: Boolean, classLabels: Map<Int, String>
) {
    val textMeasurer = rememberTextMeasurer()

    val cardCount = remember { MutableStateFlow(0) }
    val misses = remember { MutableStateFlow(0) }

    Canvas(
        modifier = Modifier.fillMaxSize()
    ) {
        val features = cardDetection?.let { it.features + it.card } ?: emptyList()

        val textStyle = TextStyle(color = Color.Green, fontSize = 12.sp, background = Color.Black.copy(alpha = 0.5f))
        val log = "Detection counts: ${cardCount.value}"
        val logLayout = textMeasurer.measure(log, textStyle)
        drawText(textLayoutResult = logLayout, topLeft = Offset(10f, 12f))


        if (cardDetection == null) {
            misses.value++
            val errorTextStyle = TextStyle(color = Color.Red, fontSize = 12.sp, background = Color.Black.copy(alpha = 0.5f))
            val errorLog = "cardDetection not detected"
            val errorLogLayout = textMeasurer.measure(errorLog, errorTextStyle)
            drawText(textLayoutResult = errorLogLayout, topLeft = Offset(10f, 60f))
        } else {
            cardCount.value++
            val infoTextStyle = TextStyle(color = Color.Green, fontSize = 12.sp, background = Color.Black.copy(alpha = 0.5f))
            val infoLog = "lockOnProgress: ${cardDetection.lockOnProgress} | id: ${cardDetection.id} | confidence: ${(cardDetection.card.confidence * 100).toInt()}%"
            val infoLogLayout = textMeasurer.measure(infoLog, infoTextStyle)
            drawText(textLayoutResult = infoLogLayout, topLeft = Offset(10f, 60f))
        }

        val infoTextStyle = TextStyle(color = Color.Red, fontSize = 12.sp, background = Color.Black.copy(alpha = 0.5f))
        val infoLog = "Misses: ${misses.value}"
        val infoLogLayout = textMeasurer.measure(infoLog, infoTextStyle)
        drawText(textLayoutResult = infoLogLayout, topLeft = Offset(10f, 108f))

        features.forEach { feature ->
            val box = feature.box.toChildSpace(imageSpaceChain)

            Log.d("DetectionOverlay2", "box: $box, classId: ${feature.classId}, confidence: ${feature.confidence}")

            drawRect(
                color = Color.Cyan,
                topLeft = Offset(box.x.toFloat(), box.y.toFloat()),
                size = Size(box.width.toFloat(), box.height.toFloat()),
                style = Stroke(width = 2.dp.toPx())
            )

            if (showClassNames) {
                val label = classLabels[feature.classId] ?: "ID: ${feature.classId}"
                val displayString = "$label (${(feature.confidence * 100).toInt()}%)"
                val labelTextStyle = TextStyle(color = Color.White, fontSize = 12.sp, background = Color.Black.copy(alpha = 0.5f))
                val textLayout = textMeasurer.measure(displayString, labelTextStyle)

                val textTop = (box.y.toFloat() - textLayout.size.height).coerceAtLeast(0f)
                drawText(textLayoutResult = textLayout, topLeft = Offset(box.x.toFloat(), textTop))
            }
        }
    }
}
