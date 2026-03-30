package com.apexfission.android.carddetectionlite.domain.tflite.filters

import com.apexfission.android.carddetectionlite.domain.tflite.model.ExtractedFeature
import kotlin.math.sqrt

/**
 * A [CardValidator] that ensures the detected object is reasonably centered in the image.
 *
 * This filter is useful for guiding the user to position the target object in the middle
 * of the camera preview. It helps prevent accidental detections of objects in the periphery.
 * The proximity is determined by calculating the distance from the center of the detection's
 * bounding box to the center of the image.
 *
 * @property maxDistancePercentage The maximum permitted distance from the center, expressed as a
 *                         percentage of the image's diagonal length. For example, a value of `0.2`
 *                         means the detection's center must be within a radius that is 20% of the
 *                         image diagonal. A smaller value creates a stricter centering requirement.
 */
class CenterProximityValidator(private val maxDistancePercentage: Float = .2f) : CardValidator {

    /**
     * Validates that the center of the [ExtractedFeature] is within the allowed proximity
     * to the center of the original image.
     *
     * This implementation uses squared distances for performance, avoiding a `sqrt` operation
     * on every check.
     *
     * @param extractedFeature The feature whose center position will be evaluated.
     * @param contextWidth (Not used by this validator).
     * @param contextHeight (Not used by this validator).
     * @param originalWidth The width of the full source image, used to find the image center.
     * @param originalHeight The height of the full source image, used to find the image center.
     * @return `true` if the detection is centered within the tolerance specified by
     *         [maxDistancePercentage], `false` otherwise.
     */
    override fun isValid(
        extractedFeature: ExtractedFeature,
        contextWidth: Int,
        contextHeight: Int,
        originalWidth: Int,
        originalHeight: Int
    ): Boolean {
        val targetWidth = originalWidth.toDouble()
        val targetHeight = originalHeight.toDouble()

        // 1. Calculate the image's diagonal and the maximum allowed distance in pixels.
        val imageDiagonal = sqrt((targetWidth * targetWidth) + (targetHeight * targetHeight))
        val maxDistanceAllowed = imageDiagonal * maxDistancePercentage
        // Use squared distance to avoid a sqrt in the distance calculation.
        val maxDistanceAllowedSq = maxDistanceAllowed * maxDistanceAllowed

        // 2. Find the center of the image.
        val imageCenterX = targetWidth / 2.0
        val imageCenterY = targetHeight / 2.0

        // 3. Find the center of the detection's bounding box.
        val coords = extractedFeature.coordinates
        val detCenterX = coords.centerX().toDouble()
        val detCenterY = coords.centerY().toDouble()

        // 4. Calculate the squared Euclidean distance between the two centers.
        val dx = detCenterX - imageCenterX
        val dy = detCenterY - imageCenterY
        val distanceSq = (dx * dx) + (dy * dy)

        // 5. Compare squared distances.
        return distanceSq <= maxDistanceAllowedSq
    }
}
