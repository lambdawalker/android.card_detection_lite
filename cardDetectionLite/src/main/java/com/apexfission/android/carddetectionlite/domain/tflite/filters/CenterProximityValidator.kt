package com.apexfission.android.carddetectionlite.domain.tflite.filters

import android.util.Log
import com.apexfission.android.carddetectionlite.domain.tflite.model.ExtractedFeature
import kotlin.math.sqrt

/**
 * A [CardValidator] that checks if the center of a detection is within a specified
 * distance from the center of the image.
 *
 * This is useful for ensuring that the detected object is near the middle of the frame,
 * which can help filter out irrelevant objects in the periphery.
 *
 * @property maxDistancePercentage The maximum allowed distance from the image center,
 *                         expressed as a percentage of the image's diagonal length.
 *                         A detection is considered invalid if the Euclidean distance between
 *                         its center and the image's center exceeds this value.
 */
class CenterProximityValidator(private val maxDistancePercentage: Float = .2f) : CardValidator {

    /**
     * Validates that the center of the [ExtractedFeature] is close to the image center.
     *
     * The maximum allowed distance is calculated based on the [maxDistancePercentage] of the image's diagonal.
     *
     * @param extractedFeature The feature to validate.
     * @param contextWidth The width of the source image.
     * @param contextHeight The height of the source image.
     * @return `true` if the distance from the feature's center to the image's center is
     *         within the allowed range, `false` otherwise.
     */
    override fun isValid(
        extractedFeature: ExtractedFeature,
        contextWidth: Int,
        contextHeight: Int,
        originalWidth: Int,
        originalHeight: Int
    ): Boolean {
        // Use originalWidth/Height if that's what the feature coordinates are mapped to
        val targetWidth = originalWidth.toDouble()
        val targetHeight = originalHeight.toDouble()

        // 1. Calculate true diagonal and max allowed distance squared
        val diagonalSquared = (targetWidth * targetWidth) + (targetHeight * targetHeight)
        val maxDistanceAllowed = sqrt(diagonalSquared) * maxDistancePercentage
        val maxDistanceAllowedSq = maxDistanceAllowed * maxDistanceAllowed

        // 2. Find centers
        val imageCenterX = targetWidth / 2.0
        val imageCenterY = targetHeight / 2.0

        val coords = extractedFeature.coordinates
        val detCenterX = (coords.left + coords.right) / 2.0
        val detCenterY = (coords.top + coords.bottom) / 2.0

        // 3. Calculate squared Euclidean distance
        val dx = detCenterX - imageCenterX
        val dy = detCenterY - imageCenterY
        val distanceSq = (dx * dx) + (dy * dy)

        return distanceSq <= maxDistanceAllowedSq
    }
}
