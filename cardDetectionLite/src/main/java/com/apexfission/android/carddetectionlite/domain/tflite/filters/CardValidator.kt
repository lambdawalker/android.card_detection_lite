package com.apexfission.android.carddetectionlite.domain.tflite.filters

import com.apexfission.android.carddetectionlite.domain.tflite.model.ExtractedFeature

/**
 * A functional interface for creating custom validation rules for detected objects.
 *
 * Implement this interface to define specific criteria that an [ExtractedFeature] must meet
 * to be considered a valid target. This allows for a flexible and composable way to
 * filter out unwanted detections based on properties like size, aspect ratio, position, etc.
 *
 * A list of these validators is typically passed to a higher-level detector, which will only
 * proceed with features that pass *all* provided validation checks.
 */
fun interface CardValidator {
    /**
     * Evaluates an [ExtractedFeature] against a specific validation rule.
     *
     * @param extractedFeature The detected object to be validated. It contains the bounding box
     *                         and other metadata of the detection.
     * @param contextWidth The width of the processed image frame (the "context"). This may be a
     *                     cropped version of the full camera image, so it represents the frame
     *                     of reference for the detection coordinates.
     * @param contextHeight The height of the processed image frame (the "context").
     * @param originalWidth The absolute width of the original, un-cropped image from the sensor.
     *                      Provided for validations that might need to consider the feature's
     *                      properties relative to the full image size.
     * @param originalHeight The absolute height of the original, un-cropped image.
     * @return `true` if the feature satisfies the validation rule, `false` otherwise.
     */
    fun isValid(extractedFeature: ExtractedFeature, contextWidth: Int, contextHeight: Int, originalWidth: Int, originalHeight: Int): Boolean
}
