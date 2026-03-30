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
    override fun isValid(
        extractedFeature: ExtractedFeature,
        contextWidth: Int,
        contextHeight: Int,
        originalWidth: Int,
        originalHeight: Int
    ): Boolean {
        if (margin <= 0) return true

        // Use the dimensions that match the feature's coordinate space.
        // Assuming coordinates are scaled to contextWidth/Height here.
        val w = contextWidth
        val h = contextHeight

        // Defensive check: Ensure margin doesn't exceed image dimensions
        if (margin * 2 >= w || margin * 2 >= h) return false

        val coordinates = extractedFeature.contextCoordinates

        return coordinates.top >= margin &&
            coordinates.left >= margin &&
            coordinates.right <= (w - margin) &&
            coordinates.bottom <= (h - margin)
    }
}