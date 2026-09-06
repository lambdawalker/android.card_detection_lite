package com.apexfission.android.carddetectionlite.domain.tflite.filters

import android.graphics.Bitmap
import com.apexfission.android.carddetectionlite.domain.tflite.model.Detection2
import kotlin.math.max
import kotlin.math.min

/**
 * A [CardValidator] that checks if a detected object's shape is plausible.
 *
 * This validator is essential for filtering out erroneously shaped detections that might have
 * high confidence scores but are clearly not the target object (e.g., a long, thin box
 * when expecting a credit card). It calculates the aspect ratio by dividing the longest side
 * of the bounding box by its shortest side.
 *
 * @property minAspectRatio The minimum acceptable ratio of the longest side to the shortest side.
 *                          For example, a value of 1.28 is suitable for standard ID cards.
 * @property maxAspectRatio The maximum acceptable ratio of the longest side to the shortest side.
 *                          For example, a value of 1.75 accommodates for some perspective skew.
 */
class AspectRatioValidator(
    private val minAspectRatio: Float = 1.28f,
    private val maxAspectRatio: Float = 1.7f
) : CardValidator {

    override fun isValid(
        detection: Detection2, previousCardDetection: Detection2?, bitmap: Bitmap
    ): Boolean {
        val width = (detection.box.x2 - detection.box.x).toFloat()
        val height = (detection.box.y2 - detection.box.y).toFloat()

        if (width <= 0 || height <= 0) {
            return false
        }

        val shortestSide = min(width, height)
        val longestSide = max(width, height)

        val aspectRatio = longestSide / shortestSide
        return aspectRatio in minAspectRatio..maxAspectRatio
    }
}
