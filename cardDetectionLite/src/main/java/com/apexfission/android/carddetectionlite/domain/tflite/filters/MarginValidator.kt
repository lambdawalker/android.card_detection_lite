package com.apexfission.android.carddetectionlite.domain.tflite.filters

import android.graphics.Bitmap
import com.apexfission.android.carddetectionlite.domain.tflite.model.Detection

/**
 * A [CardValidator] that checks if a detection is within a specified margin from the image edges.
 *
 * Useful for filtering out objects that are partially cut off at image borders.
 *
 * @property margin The required minimum distance in pixels from the image edges.
 */
class MarginValidator(private val margin: UInt = 20u) : CardValidator {
    /**
     * Validates that the candidate's bounding box is at least [margin] pixels away from image borders.
     *
     * @param detection The candidate detection to validate.
     * @param previousCardDetection The previous accepted detection, if available.
     * @param bitmap The frame image bitmap.
     * @return `true` if all sides of the box satisfy the margin requirement, `false` otherwise.
     */
    override fun isValid(
        detection: Detection, previousCardDetection: Detection?, bitmap: Bitmap
    ): Boolean {
        val w = bitmap.width.toUInt()
        val h = bitmap.height.toUInt()

        if (margin * 2u >= w || margin * 2u >= h) return false

        val box = detection.box

        return box.y >= margin &&
            box.x >= margin &&
            box.x2 <= (w - margin) &&
            box.y2 <= (h - margin)
    }
}
