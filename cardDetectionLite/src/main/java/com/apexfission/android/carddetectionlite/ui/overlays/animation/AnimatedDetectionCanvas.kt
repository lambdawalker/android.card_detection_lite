package com.apexfission.android.carddetectionlite.ui.overlays.animation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.apexfission.android.carddetectionlite.domain.coordinates.models.ImageBox
import com.apexfission.android.carddetectionlite.ui.detector.CardDetectorOverlayScope
import com.apexfission.android.carddetectionlite.ui.overlays.animation.x.rememberAnimatedDetectionBounds

/**
 * Scope wrapper providing both Compose [DrawScope] functions and real-time [AnimatedDetectionBounds].
 */
class AnimatedDetectionScope(
    private val drawScope: DrawScope,
    val bounds: AnimatedDetectionBounds
) : DrawScope by drawScope

/**
 * Renders a full-screen [Canvas] providing animated card detection bounds and state in [AnimatedDetectionScope].
 *
 * Allows developers to easily draw custom overlay graphics relative to animated card bounds.
 */
@Composable
fun CardDetectorOverlayScope.AnimatedDetectionCanvas(
    modifier: Modifier = Modifier.fillMaxSize(),
    config: DetectionAnimationConfig = DetectionAnimationConfig(),
    onDraw: AnimatedDetectionScope.() -> Unit
) {
    BoxWithConstraints(modifier = modifier) {
        val containerWidth = constraints.maxWidth.toFloat()
        val containerHeight = constraints.maxHeight.toFloat()

        val defaultCenterBox = remember(containerWidth, containerHeight) {
            val aspectRatio = 1.58577f
            var width = containerWidth * 0.85f
            var height = width / aspectRatio
            if (height > containerHeight * 0.55f) {
                height = containerHeight * 0.55f
                width = height * aspectRatio
            }
            val left = (containerWidth - width) / 2f
            val top = (containerHeight - height) / 2.2f
            ImageBox.from2P(left.toInt(), top.toInt(), (left + width).toInt(), (top + height).toInt())
        }

        val bounds = rememberAnimatedDetectionBounds(
            defaultBox = defaultCenterBox,
            config = config
        )

        Canvas(modifier = Modifier.fillMaxSize()) {
            val scope = AnimatedDetectionScope(drawScope = this, bounds = bounds)
            scope.onDraw()
        }
    }
}
