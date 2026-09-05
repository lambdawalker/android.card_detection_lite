package com.apexfission.android.carddetectionlite.domain.tflite.filters

import android.graphics.Bitmap
import com.apexfission.android.carddetectionlite.domain.coordinates.operations.arrange
import com.apexfission.android.carddetectionlite.domain.tflite.model.Detection2

/**
 * A [CardValidator] that checks if a detection is within a specified margin from the image edges.
 *
 * This is useful for filtering out objects that are partially cut off at the borders of the image.
 *
 * @property margin The minimum required distance, in pixels, from the image edges.
 *                  A detection is considered invalid if any of its sides are closer to the
 *                  corresponding image edge than this margin.
 */
class MarginValidator2(private val margin: UInt = 20u) : CardValidator2 {
    override fun isValid(
        detection: Detection2, previousCardDetection: Detection2?, bitmap: Bitmap
    ): Boolean {
        // Use the dimensions that match the feature's coordinate space.
        // Assuming coordinates are scaled to contextWidth/Height here.
        val w = bitmap.width.toUInt()
        val h = bitmap.height.toUInt()

        // Defensive check: Ensure margin doesn't exceed image dimensions
        if (margin * 2u >= w || margin * 2u >= h) return false

        val box = detection.box.arrange()

        return box.y >= margin &&
            box.x >= margin &&
            box.x2 <= (w - margin) &&
            box.y2 <= (h - margin)
    }
}