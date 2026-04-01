package com.apexfission.android.carddetectionlite.domain.tflite.model

import android.graphics.Rect

/**
 * Represents a complete, validated card detection event.
 *
 * This data class encapsulates not just the primary detected card, but also its associated
 * features, its stability ("lock-on progress"), and the context of the image in which it was found.
 *
 * @property lockOnProgress A value from 0.0 to 1.0 indicating the stability of the detection.
 *                        A value of 1.0 signifies that the card has been consistently detected
 *                        across multiple frames and is considered "locked on."
 * @property id A unique identifier for this specific card detection instance. The ID increments
 *              each time a new card is successfully "locked on."
 * @property card The [ExtractedFeature] representing the primary detected card.
 * @property features A list of other [ExtractedFeature]s that were detected within the
 *                    bounding box of the primary card.
 * @property contextSize A `Rect` representing the dimensions of the image that was processed
 *                       to find this detection (e.g., `Rect(0, 0, imageWidth, imageHeight)`).
 */
data class CardDetection(
    val lockOnProgress: Float,
    val id: Int?,
    val card: ExtractedFeature,
    val features: List<ExtractedFeature>,
    val contextSize: Rect,
    val isNewDetection: Boolean,
    val sourceSize: Rect
)
