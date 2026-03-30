package com.apexfission.android.carddetectionlite.domain.tflite.model

import android.graphics.Bitmap
import android.graphics.Rect

/**
 * Represents a "rich" version of a detected object, enhanced with concrete, usable data.
 *
 * While a `Detection` object contains raw, normalized data from the model, this class
 * transforms that data into a more useful form. It serves as the bridge between the abstract
 * output of the ML model and the concrete needs of the application, such as displaying the
 * object on screen or performing further analysis on its cropped image.
 *
 * @property detection The original [Detection] instance, containing the raw, normalized
 *           coordinate data and confidence scores.
 * @property coordinates The bounding box of the detected object, converted into an absolute pixel
 *           `Rect` relative to the **original, full-sized** image.
 * @property contextCoordinates The bounding box of the detected object, converted into an
 *           absolute pixel `Rect` relative to the **context** (potentially cropped) image.
 * @property objectBitmap A new [Bitmap] that has been cropped from the source image to show
 *           only the detected object. This is useful for UI display, logging, or as input
 *           for a subsequent ML model (e.g., OCR, barcode scanning).
 * @property confidence A convenience accessor for `detection.confidence`.
 * @property classId A convenience accessor for `detection.classId`.
 */
data class ExtractedFeature(
    val detection: Detection,
    val coordinates: Rect,
    val contextCoordinates: Rect,
    val objectBitmap: Bitmap,
    val confidence: Float = detection.confidence,
    val classId: Int = detection.classId
)

/**
 * A container for all the [ExtractedFeature]s found in a single processing frame.
 *
 * @property extractedFeatures A list of all the rich [ExtractedFeature] objects.
 * @property contextWidth The pixel width of the context image from which features were extracted.
 * @property contextHeight The pixel height of the context image.
 * @property originalWidth The pixel width of the original source image.
 * @property originalHeight The pixel height of the original source image.
 */
data class ExtractedFeatures(
    val extractedFeatures: List<ExtractedFeature>, val contextWidth: Int, val contextHeight: Int, val originalWidth: Int, val originalHeight: Int
)

/**
 * A factory function that creates a rich [ExtractedFeature] from a raw [Detection].
 *
 * This function performs the essential step of converting the abstract, normalized coordinates
 * from the ML model into a concrete, cropped bitmap of the detected object.
 *
 * @param originalBitmap The full, original source image from which the detection was made.
 * @param detection The raw [Detection] object from the post-processing step.
 * @param padding An optional value in pixels to expand the cropping area around the
 *          detection's bounding box. This can be useful to ensure the object's edges
 *          are not cut off or to capture some of the surrounding context.
 * @return A fully populated [ExtractedFeature] object.
 */
fun buildDetection(originalBitmap: Bitmap, detection: Detection, padding: Int = 0): ExtractedFeature {
    // Note: The 'coordinates' and 'contextCoordinates' might be derived from different base
    // dimensions if the originalBitmap passed here is the 'context' bitmap. The caller
    // is responsible for passing the correct bitmap that corresponds to the desired coordinate space.
    val coordinates = percentageCoordinatesToRect(
        detection, originalBitmap.width, originalBitmap.height
    )
    val contextCoordinates = percentageCoordinatesContextToRect(
        detection, originalBitmap.width, originalBitmap.height
    )

    // Apply padding and create the cropped bitmap, ensuring we don't crop outside the original bitmap's bounds.
    val paddedLeft = (coordinates.left - padding).coerceAtLeast(0)
    val paddingTop = (coordinates.top - padding).coerceAtLeast(0)
    val paddedRight = (coordinates.right + padding).coerceAtMost(originalBitmap.width)
    val paddedBottom = (coordinates.bottom + padding).coerceAtMost(originalBitmap.height)
    val paddedWidth = paddedRight - paddedLeft
    val paddedHeight = paddedBottom - paddingTop

    val cutoff = Bitmap.createBitmap(
        originalBitmap, paddedLeft, paddingTop, paddedWidth, paddedHeight
    )

    return ExtractedFeature(
        detection = detection, coordinates = coordinates, contextCoordinates = contextCoordinates, objectBitmap = cutoff
    )
}

/**
 * @private
 * Converts normalized coordinates relative to the **original** image into an absolute pixel `Rect`.
 * It includes safety checks to prevent coordinates from going out of bounds, which would
 * cause `Bitmap.createBitmap` to crash.
 */
private fun percentageCoordinatesToRect(detection: Detection, originalWidth: Int, originalHeight: Int): Rect {
    val x1 = (detection.x1Pct * originalWidth).toInt()
    val y1 = (detection.y1Pct * originalHeight).toInt()
    val x2 = (detection.x2Pct * originalWidth).toInt()
    val y2 = (detection.y2Pct * originalHeight).toInt()

    // Coerce values to be within the image bounds and ensure width/height are at least 1.
    val left = x1.coerceIn(0, originalWidth - 1)
    val top = y1.coerceIn(0, originalHeight - 1)
    val right = x2.coerceIn(left + 1, originalWidth)
    val bottom = y2.coerceIn(top + 1, originalHeight)

    return Rect(left, top, right, bottom)
}

/**
 * @private
 * Converts normalized coordinates relative to the **context** (cropped) image into an absolute pixel `Rect`.
 */
private fun percentageCoordinatesContextToRect(detection: Detection, contextWidth: Int, contextHeight: Int): Rect {
    val x1 = (detection.contextX1Pct * contextWidth).toInt()
    val y1 = (detection.contextY1Pct * contextHeight).toInt()
    val x2 = (detection.contextX2Pct * contextWidth).toInt()
    val y2 = (detection.contextY2Pct * contextHeight).toInt()

    val left = x1.coerceIn(0, contextWidth - 1)
    val top = y1.coerceIn(0, contextHeight - 1)
    val right = x2.coerceIn(left + 1, contextWidth)
    val bottom = y2.coerceIn(top + 1, contextHeight)

    return Rect(left, top, right, bottom)
}
