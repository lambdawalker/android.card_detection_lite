package com.apexfission.android.carddetectionlite.ui.overlays

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.apexfission.android.carddetectionlite.domain.coordinates.models.ImageBox
import com.apexfission.android.carddetectionlite.domain.coordinates.models.ImageSpaceChain
import com.apexfission.android.carddetectionlite.domain.coordinates.transformations.translate
import com.apexfission.android.carddetectionlite.domain.tflite.detector.InputShape
import kotlin.math.roundToInt

/**
 * An overlay Composable that renders the Area of Interest (ROI) corresponding to the [InputShape]
 * configuration, darkening the parts of the camera preview not included in the cropped region.
 *
 * @param inputShape The current [InputShape] cropping strategy.
 * @param imageSpaceChain Coordinate transformation chain mapping bitmap space to overlay/screen space.
 * @param scrimColor Color used to darken the inactive area outside the area of interest.
 * @param borderColor Color of the border surrounding the area of interest.
 * @param borderWidth Width of the border stroke.
 */
@Composable
fun AreaOfInterest(
    inputShape: InputShape,
    imageSpaceChain: ImageSpaceChain,
    scrimColor: Color = Color.Black.copy(alpha = 0.5f),
    borderColor: Color = Color.White.copy(alpha = 0.0f),
    borderWidth: Dp = 2.dp
) {
    if (imageSpaceChain.isEmpty()) return

    val srcWidth = imageSpaceChain.first().space.width.toInt()
    val srcHeight = imageSpaceChain.first().space.height.toInt()

    Canvas(modifier = Modifier.fillMaxSize()) {
        val canvasWidth = size.width.toInt()
        val canvasHeight = size.height.toInt()
        val density = android.content.res.Resources.getSystem().displayMetrics.density

        val cropBitmapBox = when (inputShape) {
            is InputShape.FullImage -> {
                ImageBox.fromPS(0, 0, srcWidth, srcHeight)
            }
            is InputShape.CenterSquareCrop -> {
                val cropSize = minOf(srcWidth, srcHeight)
                val left = (srcWidth - cropSize) / 2
                val top = (srcHeight - cropSize) / 2
                ImageBox.fromPS(left, top, cropSize, cropSize)
            }
            is InputShape.SquareCrop -> {
                val cropSize = minOf(srcWidth, srcHeight)
                val left = (srcWidth - cropSize) / 2
                val defaultTop = (srcHeight - cropSize) / 2
                val topPx = (inputShape.top.value * density).roundToInt()
                val finalTop = (defaultTop + topPx).coerceIn(0, (srcHeight - cropSize).coerceAtLeast(0))
                ImageBox.fromPS(left, finalTop, cropSize, cropSize)
            }
            is InputShape.CenterVisibleImage -> {
                getAspectRatioCropRect(srcWidth, srcHeight, canvasWidth, canvasHeight, square = false, top = 0.dp, density = density)
            }
            is InputShape.VisibleImage -> {
                getAspectRatioCropRect(srcWidth, srcHeight, canvasWidth, canvasHeight, square = false, top = inputShape.top, density = density)
            }
            is InputShape.CenterVisibleImageSquareCrop -> {
                getAspectRatioCropRect(srcWidth, srcHeight, canvasWidth, canvasHeight, square = true, top = 0.dp, density = density)
            }
            is InputShape.VisibleImageSquareCrop -> {
                getAspectRatioCropRect(srcWidth, srcHeight, canvasWidth, canvasHeight, square = true, top = inputShape.top, density = density)
            }
        }

        val canvasBox = cropBitmapBox.translate(imageSpaceChain)

        val boxLeft = canvasBox.x.toFloat()
        val boxTop = canvasBox.y.toFloat()
        val boxRight = canvasBox.x2.toFloat()
        val boxBottom = canvasBox.y2.toFloat()
        val boxWidth = canvasBox.width.toFloat()
        val boxHeight = canvasBox.height.toFloat()

        // 1. Draw top scrim
        if (boxTop > 0f) {
            drawRect(
                color = scrimColor,
                topLeft = Offset(0f, 0f),
                size = Size(size.width, boxTop)
            )
        }

        // 2. Draw bottom scrim
        if (boxBottom < size.height) {
            drawRect(
                color = scrimColor,
                topLeft = Offset(0f, boxBottom),
                size = Size(size.width, (size.height - boxBottom).coerceAtLeast(0f))
            )
        }

        // 3. Draw left scrim
        if (boxLeft > 0f) {
            drawRect(
                color = scrimColor,
                topLeft = Offset(0f, boxTop),
                size = Size(boxLeft, boxHeight)
            )
        }

        // 4. Draw right scrim
        if (boxRight < size.width) {
            drawRect(
                color = scrimColor,
                topLeft = Offset(boxRight, boxTop),
                size = Size((size.width - boxRight).coerceAtLeast(0f), boxHeight)
            )
        }

        // 5. Draw border around Area of Interest
        drawRect(
            color = borderColor,
            topLeft = Offset(boxLeft, boxTop),
            size = Size(boxWidth, boxHeight),
            style = Stroke(width = borderWidth.toPx())
        )
    }
}

private fun getAspectRatioCropRect(
    srcWidth: Int,
    srcHeight: Int,
    canvasWidth: Int,
    canvasHeight: Int,
    square: Boolean,
    top: Dp,
    density: Float
): ImageBox {
    val fSrcWidth = srcWidth.toFloat()
    val fSrcHeight = srcHeight.toFloat()
    val targetAspectRatio = if (canvasHeight > 0) canvasWidth.toFloat() / canvasHeight.toFloat() else 1f
    val srcAspectRatio = fSrcWidth / fSrcHeight

    var finalCropWidth = fSrcWidth
    var finalCropHeight = fSrcHeight

    if (srcAspectRatio > targetAspectRatio) {
        finalCropWidth = fSrcHeight * targetAspectRatio
    } else if (srcAspectRatio < targetAspectRatio) {
        finalCropWidth = fSrcWidth / targetAspectRatio
    }

    if (square) {
        val size = minOf(finalCropWidth, finalCropHeight)
        finalCropWidth = size
        finalCropHeight = size
    }

    val xOffset = ((fSrcWidth - finalCropWidth) / 2).roundToInt()
    val defaultYOffset = ((fSrcHeight - finalCropHeight) / 2).roundToInt()
    val topPx = (top.value * density).roundToInt()
    val yOffset = (defaultYOffset + topPx).coerceIn(0, (fSrcHeight - finalCropHeight).roundToInt().coerceAtLeast(0))

    return ImageBox.fromPS(
        xOffset.coerceIn(0, (fSrcWidth - finalCropWidth).roundToInt().coerceAtLeast(0)),
        yOffset,
        finalCropWidth.roundToInt().coerceAtMost(srcWidth - xOffset),
        finalCropHeight.roundToInt().coerceAtMost(srcHeight - yOffset)
    )
}
