package com.apexfission.android.carddetectionlite.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import com.apexfission.android.carddetectionlite.domain.tflite.model.CardDetection
import kotlin.math.max

@Composable
fun CardLockOnOverlay(
    lockOnProgress: Float,
    activeDetection: CardDetection?,
    scalingInfo: PreviewScalingInfo
) {
    val card = activeDetection?.card ?: return
    val det = card.detection

    val smoothX1 by animateFloatAsState(targetValue = det.x1Pct, label = "x1", animationSpec = spring(stiffness = Spring.StiffnessLow))
    val smoothY1 by animateFloatAsState(targetValue = det.y1Pct, label = "y1", animationSpec = spring(stiffness = Spring.StiffnessLow))
    val smoothX2 by animateFloatAsState(targetValue = det.x2Pct, label = "x2", animationSpec = spring(stiffness = Spring.StiffnessLow))
    val smoothY2 by animateFloatAsState(targetValue = det.y2Pct, label = "y2", animationSpec = spring(stiffness = Spring.StiffnessLow))

    val smoothProgress by animateFloatAsState(targetValue = lockOnProgress, label = "progress")

    Canvas(modifier = Modifier.fillMaxSize()) {
        val screenW = size.width
        val screenH = size.height

        val scale = max(screenW / scalingInfo.fullW, screenH / scalingInfo.fullH)
        val offsetX = (screenW - scalingInfo.fullW * scale) / 2f
        val offsetY = (screenH - scalingInfo.fullH * scale) / 2f

        // Use the SMOOTHED values for the pixel calculation
        val x1 = (smoothX1 * scalingInfo.cropW) * scale + offsetX
        val y1 = (smoothY1 * scalingInfo.cropH) * scale + offsetY
        val x2 = (smoothX2 * scalingInfo.cropW) * scale + offsetX
        val y2 = (smoothY2 * scalingInfo.cropH) * scale + offsetY

        // Shrinking frame calculations
        val maxOffset = 40.dp.toPx()
        val currentOffset = maxOffset * (1f - smoothProgress)
        val strokeColor = lerp(Color.White.copy(alpha = 0.4f), Color.Green, smoothProgress)
        val strokeWidth = 4.dp.toPx()
        val cornerLength = 32.dp.toPx()

        val animLeft = x1 - currentOffset
        val animTop = y1 - currentOffset
        val animRight = x2 + currentOffset
        val animBottom = y2 + currentOffset

        val bracketPath = Path().apply {
            // Top-Left
            moveTo(animLeft, animTop + cornerLength)
            lineTo(animLeft, animTop)
            lineTo(animLeft + cornerLength, animTop)
            // Top-Right
            moveTo(animRight - cornerLength, animTop)
            lineTo(animRight, animTop)
            lineTo(animRight, animTop + cornerLength)
            // Bottom-Right
            moveTo(animRight, animBottom - cornerLength)
            lineTo(animRight, animBottom)
            lineTo(animRight - cornerLength, animBottom)
            // Bottom-Left
            moveTo(animLeft + cornerLength, animBottom)
            lineTo(animLeft, animBottom)
            lineTo(animLeft, animBottom - cornerLength)
        }

        drawPath(
            path = bracketPath,
            color = strokeColor,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}