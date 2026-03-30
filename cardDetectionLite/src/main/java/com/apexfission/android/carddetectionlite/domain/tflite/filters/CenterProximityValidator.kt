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
    override fun isValid(extractedFeature: ExtractedFeature, contextWidth: Int, contextHeight: Int, originalWidth: Int, originalHeight: Int): Boolean {
        val maxDistance = sqrt(contextWidth.toDouble() * contextHeight) * maxDistancePercentage

        // 1. Find the center of the image
        val imageCenterX = originalWidth / 2f
        val imageCenterY = originalHeight / 2f

        // 2. Find the center of the detection box
        val detCenterX = (extractedFeature.coordinates.left + extractedFeature.coordinates.right) / 2f
        val detCenterY = (extractedFeature.coordinates.top + extractedFeature.coordinates.bottom) / 2f

        // 3. Calculate Euclidean distance
        val dx = detCenterX - imageCenterX
        val dy = detCenterY - imageCenterY
        val distance = sqrt(dx * dx + dy * dy)

        return distance <= maxDistance
    }
}
