package com.apexfission.android.carddetectionlite.domain.tflite.filters

import com.apexfission.android.carddetectionlite.domain.tflite.model.ExtractedFeature
import kotlin.math.max
import kotlin.math.min

/**
 * A [CardValidator] that filters detections based on their aspect ratio.
 *
 * This validator is useful for ensuring that a detected object has a shape consistent
 * with the object of interest (e.g., a driver license). The aspect ratio is calculated
 * as the ratio of the longer side to the shorter side of the bounding box.
 *
 * @property minAspectRatio The minimum allowed aspect ratio (longer side / shorter side).
 * @property maxAspectRatio The maximum allowed aspect ratio (longer side / shorter side).
 */
class AspectRatioValidator(
    private val minAspectRatio: Float = 1.4f,
    private val maxAspectRatio: Float = 1.8f
) : CardValidator {
    /**
     * Validates that the aspect ratio of the [ExtractedFeature] is within the specified range.
     *
     * @param extractedFeature The feature to validate.
     * @param contextWidth The width of the source image (not used by this validator).
     * @param contextHeight The height of the source image (not used by this validator).
     * @return `true` if the feature's aspect ratio is between [minAspectRatio] and [maxAspectRatio], `false` otherwise.
     */
    override fun isValid(extractedFeature: ExtractedFeature, contextWidth: Int, contextHeight: Int, originalWidth: Int, originalHeight: Int): Boolean {
        val width = extractedFeature.coordinates.width().toDouble()
        val height = extractedFeature.coordinates.height().toDouble()

        if (width <= 0 || height <= 0) {
            return false
        }

        val u = min(width, height)
        val v = max(width, height)

        val aspectRatio = v / u
        return aspectRatio in minAspectRatio..maxAspectRatio
    }
}
