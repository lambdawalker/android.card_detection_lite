package com.apexfission.android.carddetectionlite.domain.tflite.model

import android.graphics.Bitmap
import com.apexfission.android.carddetectionlite.domain.coordinates.models.ImageBox

/**
 * Represents a raw object detection output from the model.
 *
 * @property box Bounding box coordinates in absolute pixel space.
 * @property confidence Confidence score of the detection from 0.0 to 1.0.
 * @property classId The integer ID of the detected class.
 */
data class Detection(
    val box: ImageBox,
     val confidence: Float,
    val classId: Int
)

/**
 * Represents a detected object feature along with its source image bitmap.
 *
 * @property box Bounding box coordinates in absolute pixel space.
 * @property confidence Confidence score of the detection from 0.0 to 1.0.
 * @property classId The integer ID of the detected class.
 * @property image The source bitmap containing the cropped feature image or full frame.
 */
data class Feature(
    val box: ImageBox,
    val confidence: Float,
    val classId: Int,
    val image: Bitmap
)
