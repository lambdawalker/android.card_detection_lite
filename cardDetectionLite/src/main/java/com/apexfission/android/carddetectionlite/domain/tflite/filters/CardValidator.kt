package com.apexfission.android.carddetectionlite.domain.tflite.filters

import android.graphics.Bitmap
import com.apexfission.android.carddetectionlite.domain.tflite.model.Detection

/**
 * A functional interface for creating custom validation rules for detected objects.
 *
 * Implement this interface to define specific criteria that a [Detection] candidate must meet
 * to be considered a valid target. This allows for a flexible and composable way to
 * filter out unwanted detections based on properties like size, aspect ratio, position, etc.
 */
fun interface CardValidator {
    /**
     * Stable identity for detector configuration. Custom validators should override this
     * when equivalent instances can be recreated during recomposition. Otherwise the
     * validator instance identity is used and callers should retain it with `remember`.
     */
    val configurationKey: String
        get() = "${javaClass.name}@${System.identityHashCode(this)}"

    /**
     * Evaluates a [Detection] candidate against a specific validation rule.
     *
     * @param detection The candidate detection to validate.
     * @param previousCardDetection The previous accepted detection frame, if available.
     * @param bitmap The frame image bitmap.
     * @return `true` if the candidate satisfies the validation rule, `false` otherwise.
     */
    fun isValid(detection: Detection, previousCardDetection: Detection?, bitmap: Bitmap): Boolean
}
