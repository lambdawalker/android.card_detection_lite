package com.apexfission.android.carddetectionlite.domain.tflite.model

/**
 * Represents a raw detection with coordinates normalized to its immediate reference frame (the "context").
 *
 * This is an intermediate representation used during post-processing.
 *
 * @property x1Pct The normalized (0.0 to 1.0) X coordinate of the top-left corner, relative to the context width.
 * @property y1Pct The normalized (0.0 to 1.0) Y coordinate of the top-left corner, relative to the context height.
 * @property x2Pct The normalized (0.0 to 1.0) X coordinate of the bottom-right corner.
 * @property y2Pct The normalized (0.0 to 1.0) Y coordinate of the bottom-right corner.
 * @property confidence The confidence score of the detection (typically 0.0 to 1.0).
 * @property classId The integer ID of the detected class.
 */
data class RawDetection(val x1Pct: Float, val y1Pct: Float, val x2Pct: Float, val y2Pct: Float, val confidence: Float, val classId: Int)

/**
 * Represents a final, processed detection with coordinates in multiple reference frames.
 *
 * This class provides bounding box coordinates normalized to both the original, full-sized image
 * and the intermediate, cropped "context" image. This is essential for correctly rendering
 * overlays on a UI that might be displaying a cropped view of the full camera feed.
 *
 * @property x1Pct The X coordinate of the top-left corner, normalized to the **original** image width.
 * @property y1Pct The Y coordinate of the top-left corner, normalized to the **original** image height.
 * @property x2Pct The X coordinate of the bottom-right corner, normalized to the **original** image width.
 * @property y2Pct The Y coordinate of the bottom-right corner, normalized to the **original** image height.
 * @property contextX1Pct The X coordinate of the top-left corner, normalized to the **context** (cropped) image width.
 * @property contextY1Pct The Y coordinate of the top-left corner, normalized to the **context** (cropped) image height.
 * @property contextX2Pct The X coordinate of the bottom-right corner, normalized to the **context** (cropped) image width.
 * @property contextY2Pct The Y coordinate of the bottom-right corner, normalized to the **context** (cropped) image height.
 * @property confidence The confidence score of the detection.
 * @property classId The integer ID of the detected class.
 */
data class Detection(
    val x1Pct: Float, val y1Pct: Float, val x2Pct: Float, val y2Pct: Float,
    val contextX1Pct: Float, val contextY1Pct: Float, val contextX2Pct: Float, val contextY2Pct: Float,
    val confidence: Float, val classId: Int
)

/**
 * A container for all detections found in a single image frame, along with metadata about the
 * coordinate systems involved.
 *
 * @property detections A list of final [Detection] objects.
 * @property contextWidth The pixel width of the context image (the cropped input to the model pipeline).
 * @property contextHeight The pixel height of the context image.
 * @property originalWidth The pixel width of the original, un-cropped source image.
 * @property originalHeight The pixel height of the original, un-cropped source image.
 */
data class Detections(
    val detections: List<Detection>,
    val contextWidth: Int,
    val contextHeight: Int,
    val originalWidth: Int,
    val originalHeight: Int
)
