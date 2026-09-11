package com.apexfission.android.carddetectionlite.ui.overlays

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
import com.apexfission.android.carddetectionlite.domain.coordinates.transformations.translate
import com.apexfission.android.carddetectionlite.domain.tflite.model.CardDetection
import com.apexfission.android.carddetectionlite.ui.detector.CardDetectorOverlayScope
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Scoped variant of [DetectionOverlay] using [CardDetectorOverlayScope].
 *
 * @param showClassNames Whether to draw class names above bounding boxes.
 * @param classLabels Map of class IDs to human-readable labels.
 */
@Composable
fun CardDetectorOverlayScope.DetectionOverlay(
    showClassNames: Boolean = false,
    classLabels: Map<Int, String> = this.classLabels
) {
    val spaceChain = imageSpaceChain ?: return
    DetectionOverlay(
        cardDetection = detectionState,
        imageSpaceChain = spaceChain,
        showClassNames = showClassNames,
        classLabels = classLabels
    )
}

/**
 * Standalone variant of [DetectionOverlay] taking explicit parameters.
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
            val box = feature.box.translate(imageSpaceChain)

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
