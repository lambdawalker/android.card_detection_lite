package com.apexfission.android.carddetectionlite.domain.tflite.filters

import com.apexfission.android.carddetectionlite.domain.tflite.data.Detection
import kotlin.math.max
import kotlin.math.min

/**
 * An interface for filtering a list of detected objects.
 */
fun interface DetectionFilter {
    fun filter(detection: Detection, imageWidth: Int, imageHeight: Int): Boolean
}

/**
 * Filters detections to ensure they are a certain margin away from the image edges.
 *
 * @param margin The minimum distance a detection's bounding box must be from any edge.
 */
class MarginFilter(private val margin: Int = 20) : DetectionFilter {
    override fun filter(detection: Detection, imageWidth: Int, imageHeight: Int): Boolean {
        if (margin <= 0) return true

        return detection.coordinates.top >= margin &&
            detection.coordinates.left >= margin &&
            detection.coordinates.right <= imageWidth - margin &&
            detection.coordinates.bottom <= imageHeight - margin

    }
}

/**
 * Filters detections based on their aspect ratio.
 *
 * @param minAspectRatio The minimum aspect ratio (v/u) to keep.
 * @param maxAspectRatio The maximum aspect ratio (v/u) to keep.
 */
class AspectRatioFilter(
    private val minAspectRatio: Float = 1.4f,
    private val maxAspectRatio: Float = 1.8f
) : DetectionFilter {
    override fun filter(detection: Detection, imageWidth: Int, imageHeight: Int): Boolean {
        val x1 = detection.coordinates.left
        val y1 = detection.coordinates.top
        val x2 = detection.coordinates.right
        val y2 = detection.coordinates.bottom

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