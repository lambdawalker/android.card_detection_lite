package com.apexfission.android.carddetectionlite.domain.tflite.filters

import android.graphics.Bitmap
import com.apexfission.android.carddetectionlite.domain.tflite.model.Detection2

/**
 * A functional interface for creating custom validation rules for detected objects.
 *
 * Implement this interface to define specific criteria that an [Detection2] must meet
 * to be considered a valid target. This allows for a flexible and composable way to
 * filter out unwanted detections based on properties like size, aspect ratio, position, etc.
 *
 * A list of these validators is typically passed to a higher-level detector, which will only
 * proceed with features that pass *all* provided validation checks.
 */
fun interface CardValidator {
    fun isValid(detection: Detection2, previousCardDetection: Detection2?, bitmap: Bitmap): Boolean
}
