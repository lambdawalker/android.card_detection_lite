package com.apexfission.android.carddetectionlite.domain.tflite.filters

import android.graphics.Bitmap
import com.apexfission.android.carddetectionlite.domain.tflite.model.Detection
import kotlin.math.max
import kotlin.math.min

/**
 * A [CardValidator] that checks if a detected object's shape is plausible based on aspect ratio.
 *
 * Calculates aspect ratio by dividing the longest side of the bounding box by its shortest side.
 *
 * @property minAspectRatio The minimum acceptable ratio of longest to shortest side (default 1.28).
 * @property maxAspectRatio The maximum acceptable ratio of longest to shortest side (default 1.70).
 */
class AspectRatioValidator(
    private val minAspectRatio: Float = 1.28f,
    private val maxAspectRatio: Float = 1.7f
) : CardValidator {
    override val configurationKey: String =
        "aspect-ratio:${minAspectRatio.toRawBits()}:${maxAspectRatio.toRawBits()}"


    /**
     * Validates that the aspect ratio of the candidate's bounding box falls within `minAspectRatio..maxAspectRatio`.
     *
     * @param detection The candidate detection to validate.
     * @param previousCardDetection The previous accepted detection, if available.
     * @param bitmap The frame image bitmap.
     * @return `true` if the aspect ratio is within range, `false` otherwise.
     */
    override fun isValid(
        detection: Detection, previousCardDetection: Detection?, bitmap: Bitmap
    ): Boolean {
        val width = (detection.box.x2 - detection.box.x).toFloat()
        val height = (detection.box.y2 - detection.box.y).toFloat()

        if (width <= 0f || height <= 0f) {
            return false
        }

        val shortestSide = min(width, height)
        val longestSide = max(width, height)

        val aspectRatio = longestSide / shortestSide
        return aspectRatio in minAspectRatio..maxAspectRatio
    }
}
