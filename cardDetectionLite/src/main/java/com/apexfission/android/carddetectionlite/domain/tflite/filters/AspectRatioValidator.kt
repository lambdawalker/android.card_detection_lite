package com.apexfission.android.carddetectionlite.domain.tflite.filters

import com.apexfission.android.carddetectionlite.domain.tflite.model.ExtractedFeature
import kotlin.math.max
import kotlin.math.min

/**
 * A [CardValidator] that checks if a detected object's shape is plausible.
 *
 * This validator is essential for filtering out erroneously shaped detections that might have
 * high confidence scores but are clearly not the target object (e.g., a long, thin box
 * when expecting a credit card). It calculates the aspect ratio by dividing the longest side
 * of the bounding box by its shortest side.
 *
 * @property minAspectRatio The minimum acceptable ratio of the longest side to the shortest side.
 *                          For example, a value of 1.4 is suitable for standard ID cards.
 * @property maxAspectRatio The maximum acceptable ratio of the longest side to the shortest side.
 *                          For example, a value of 1.8 accommodates for some perspective skew.
 */
class AspectRatioValidator(
    private val minAspectRatio: Float = 1.4f,
    private val maxAspectRatio: Float = 1.8f
) : CardValidator {
    /**
     * Validates that the aspect ratio of the [ExtractedFeature]'s bounding box falls
     * within the configured `min` and `max` range.
     *
     * @param extractedFeature The feature whose bounding box will be evaluated.
     * @param contextWidth (Not used by this validator)
     * @param contextHeight (Not used by this validator)
     * @param originalWidth (Not used by this validator)
     * @param originalHeight (Not used by this validator)
     * @return `true` if the aspect ratio is within the valid range, `false` otherwise. Returns
     *         `false` if the feature has a non-positive width or height.
     */
    override fun isValid(
        extractedFeature: ExtractedFeature,
        contextWidth: Int,
        contextHeight: Int,
        originalWidth: Int,
        originalHeight: Int
    ): Boolean {
        val width = extractedFeature.coordinates.width().toDouble()
        val height = extractedFeature.coordinates.height().toDouble()

        if (width <= 0 || height <= 0) {
            return false
        }

        val shortestSide = min(width, height)
        val longestSide = max(width, height)

        val aspectRatio = longestSide / shortestSide
        return aspectRatio >= minAspectRatio && aspectRatio <= maxAspectRatio
    }
}
