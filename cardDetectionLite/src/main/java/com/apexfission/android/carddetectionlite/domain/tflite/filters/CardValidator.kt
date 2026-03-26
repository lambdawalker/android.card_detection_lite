package com.apexfission.android.carddetectionlite.domain.tflite.filters

import com.apexfission.android.carddetectionlite.domain.tflite.model.ExtractedFeature
import kotlin.math.max
import kotlin.math.min

/**
 * An interface for filtering a list of detected objects.
 */
fun interface CardValidator {
    fun isValid(extractedFeature: ExtractedFeature, imageWidth: Int, imageHeight: Int): Boolean
}

/**
 * Filters detections to ensure they are a certain margin away from the image edges.
 *
 * @param margin The minimum distance a detection's bounding box must be from any edge.
 */
class MarginValidator(private val margin: Int = 20) : CardValidator {
    override fun isValid(extractedFeature: ExtractedFeature, imageWidth: Int, imageHeight: Int): Boolean {
        if (margin <= 0) return true

        return extractedFeature.coordinates.top >= margin &&
            extractedFeature.coordinates.left >= margin &&
            extractedFeature.coordinates.right <= imageWidth - margin &&
            extractedFeature.coordinates.bottom <= imageHeight - margin

    }
}

/**
 * Filters detections based on their aspect ratio.
 *
 * @param minAspectRatio The minimum aspect ratio (v/u) to keep.
 * @param maxAspectRatio The maximum aspect ratio (v/u) to keep.
 */
class AspectRatioValidator(
    private val minAspectRatio: Float = 1.4f,
    private val maxAspectRatio: Float = 1.8f
) : CardValidator {
    override fun isValid(extractedFeature: ExtractedFeature, imageWidth: Int, imageHeight: Int): Boolean {
        val x1 = extractedFeature.coordinates.left
        val y1 = extractedFeature.coordinates.top
        val x2 = extractedFeature.coordinates.right
        val y2 = extractedFeature.coordinates.bottom

        val width = x2 - x1
        val height = y2 - y1
        val u = min(width, height).toFloat()
        val v = max(width, height).toFloat()

        if (u > 0) {
            val aspectRatio = v / u
            return aspectRatio in minAspectRatio..maxAspectRatio
        } else {
            return false
        }
    }
}

/**
 * Validates that the center of the detection is within a certain distance
 * from the center of the image.
 *
 * @param maxDistancePx The maximum allowed distance in pixels.
 */
class CenterProximityValidator(private val maxDistancePx: Float = 100f) : CardValidator {

    override fun isValid(extractedFeature: ExtractedFeature, imageWidth: Int, imageHeight: Int): Boolean {
        // 1. Find the center of the image
        val imageCenterX = imageWidth / 2f
        val imageCenterY = imageHeight / 2f

        // 2. Find the center of the detection box
        val detCenterX = (extractedFeature.coordinates.left + extractedFeature.coordinates.right) / 2f
        val detCenterY = (extractedFeature.coordinates.top + extractedFeature.coordinates.bottom) / 2f

        // 3. Calculate Euclidean distance
        val dx = detCenterX - imageCenterX
        val dy = detCenterY - imageCenterY
        val distance = kotlin.math.sqrt(dx * dx + dy * dy)

        return distance <= maxDistancePx
    }
}