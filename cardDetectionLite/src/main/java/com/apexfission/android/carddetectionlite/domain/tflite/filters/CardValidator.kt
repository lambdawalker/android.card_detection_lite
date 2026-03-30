package com.apexfission.android.carddetectionlite.domain.tflite.filters

import com.apexfission.android.carddetectionlite.domain.tflite.model.ExtractedFeature

/**
 * A functional interface for validating if a detected object meets specific criteria.
 *
 * Implement this interface to create custom filters for `ExtractedFeature` objects.
 * These validators can be used to filter detections based on properties like size,
 * position, aspect ratio, or any other custom logic.
 */
fun interface CardValidator {
    /**
     * Determines if the given `ExtractedFeature` is valid.
     *
     * @param extractedFeature The detected feature to validate.
     * @param contextWidth The width of the image in which the feature was detected.
     * @param contextHeight The height of the image in which the feature was detected.
     * @return `true` if the feature is valid, `false` otherwise.
     */
    fun isValid(extractedFeature: ExtractedFeature, contextWidth: Int, contextHeight: Int, originalWidth: Int, originalHeight: Int): Boolean
}

