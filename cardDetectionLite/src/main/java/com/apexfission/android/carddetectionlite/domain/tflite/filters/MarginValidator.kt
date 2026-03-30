package com.apexfission.android.carddetectionlite.domain.tflite.filters

import com.apexfission.android.carddetectionlite.domain.tflite.model.ExtractedFeature

/**
 * A [CardValidator] that checks if a detection is within a specified margin from the image edges.
 *
 * This is useful for filtering out objects that are partially cut off at the borders of the image.
 *
 * @property margin The minimum required distance, in pixels, from the image edges.
 *                  A detection is considered invalid if any of its sides are closer to the
 *                  corresponding image edge than this margin.
 */
class MarginValidator(private val margin: Int = 20) : CardValidator {
    /**
     * Validates that the [com.apexfission.android.carddetectionlite.domain.tflite.model.ExtractedFeature] is within the specified margin.
     *
     * @param extractedFeature The feature to validate.
     * @param contextWidth The width of the source image.
     * @param contextHeight The height of the source image.
     * @return `true` if the feature's bounding box is entirely within the defined margins, `false` otherwise.
     */
    override fun isValid(extractedFeature: ExtractedFeature, contextWidth: Int, contextHeight: Int, originalWidth: Int, originalHeight: Int): Boolean {
        if (margin <= 0) return true

        return extractedFeature.coordinates.top >= margin &&
            extractedFeature.coordinates.left >= margin &&
            extractedFeature.coordinates.right <= contextWidth - margin &&
            extractedFeature.coordinates.bottom <= contextHeight - margin
    }
}