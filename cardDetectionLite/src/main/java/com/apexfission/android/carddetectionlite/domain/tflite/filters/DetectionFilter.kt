package com.apexfission.android.carddetectionlite.domain.tflite.filters

import com.apexfission.android.carddetectionlite.domain.tflite.data.Det
import kotlin.math.max
import kotlin.math.min

/**
 * An interface for filtering a list of detected objects.
 */
fun interface DetectionFilter {
    fun filter(detections: List<Det>, imageWidth: Int, imageHeight: Int): List<Det>
}

/**
 * Filters detections to ensure they are a certain margin away from the image edges.
 *
 * @param margin The minimum distance a detection's bounding box must be from any edge.
 */
class MarginFilter(private val margin: Int = 20) : DetectionFilter {
    override fun filter(detections: List<Det>, imageWidth: Int, imageHeight: Int): List<Det> {
        if (margin <= 0) return detections
        return detections.filter {
            it.x1 >= margin &&
            it.y1 >= margin &&
            it.x2 <= imageWidth - margin &&
            it.y2 <= imageHeight - margin
        }
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
    override fun filter(detections: List<Det>, imageWidth: Int, imageHeight: Int): List<Det> {
        return detections.filter {
            val width = it.x2 - it.x1
            val height = it.y2 - it.y1
            val u = min(width, height)
            val v = max(width, height)
            if (u > 0) {
                val aspectRatio = v / u
                aspectRatio in minAspectRatio..maxAspectRatio
            } else {
                false
            }
        }
    }
}