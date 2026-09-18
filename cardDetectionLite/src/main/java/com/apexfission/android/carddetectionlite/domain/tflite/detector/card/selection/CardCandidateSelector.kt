package com.apexfission.android.carddetectionlite.domain.tflite.detector.card.selection

import android.graphics.Bitmap
import com.apexfission.android.yolo.postprocess.intersectionOverUnion
import com.apexfission.android.carddetectionlite.domain.tflite.filters.CardValidator
import com.apexfission.android.yolo.engine.Detection

/**
 * Handles filtering raw model detections and selecting the best spatial candidate matching previous tracking state.
 */
internal class CardCandidateSelector(
    private val cardClasses: Set<Int>,
    private val cardValidators: List<CardValidator>,
    private val validateClassIdInLockOnProcess: Boolean = true
) {

    init {
        require(cardClasses.isNotEmpty()) {
            "cardClasses must not be empty"
        }
    }

    /**
     * Filters [detections] using [cardClasses] and [cardValidators], then selects the candidate
     * with the highest spatial overlap (IoU) or highest confidence.
     */
    fun selectCandidate(
        detections: List<Detection>,
        previousDetection: Detection?,
        bitmap: Bitmap
    ): Detection? {
        val candidates = detections.filter { detection ->
            detection.classId in cardClasses &&
                cardValidators.all { validator ->
                    validator.isValid(
                        detection,
                        previousDetection,
                        bitmap
                    )
                }
        }

        if (candidates.isEmpty()) {
            return null
        }

        if (previousDetection == null) {
            return candidates.maxByOrNull { it.confidence }
        }

        val preferredCandidates =
            if (validateClassIdInLockOnProcess) {
                candidates
                    .filter { it.classId == previousDetection.classId }
                    .ifEmpty { candidates }
            } else {
                candidates
            }

        val bestSpatialCandidate =
            preferredCandidates.maxByOrNull {
                intersectionOverUnion(
                    it,
                    previousDetection
                )
            }

        if (bestSpatialCandidate != null) {
            val overlap = intersectionOverUnion(
                bestSpatialCandidate,
                previousDetection
            )

            if (overlap > 0.0) {
                return bestSpatialCandidate
            }
        }

        return candidates.maxByOrNull { it.confidence }
    }
}
