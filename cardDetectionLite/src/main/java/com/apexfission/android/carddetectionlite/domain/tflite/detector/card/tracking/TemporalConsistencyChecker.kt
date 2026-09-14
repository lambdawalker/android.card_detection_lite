package com.apexfission.android.carddetectionlite.domain.tflite.detector.card.tracking

import com.apexfission.android.carddetectionlite.domain.tflite.image.isVisuallySimilar
import com.apexfission.android.carddetectionlite.domain.tflite.model.Detection

/**
 * Determines whether a candidate detection is visually and categorically consistent with
 * the previous frame's candidate sequence.
 */
internal class TemporalConsistencyChecker(
    private val validateClassIdInLockOnProcess: Boolean = true,
    private val differenceHashDistanceLimit: Int = 25
) {

    init {
        require(differenceHashDistanceLimit in 0..64) {
            "differenceHashDistanceLimit must be between 0 and 64"
        }
    }

    /**
     * Returns `true` if [candidate] continues the candidate sequence represented by [previousDetection]
     * and [previousComparisonHash].
     */
    fun isConsistent(
        candidate: Detection,
        currentHash: ULong,
        previousDetection: Detection?,
        previousComparisonHash: ULong?
    ): Boolean {
        val hasPreviousCandidate = previousDetection != null && previousComparisonHash != null

        val classMatches = !validateClassIdInLockOnProcess ||
            previousDetection == null ||
            candidate.classId == previousDetection.classId

        val visuallyMatches = previousComparisonHash?.let { hash ->
            isVisuallySimilar(
                hash,
                currentHash,
                differenceHashDistanceLimit
            )
        } ?: false

        return hasPreviousCandidate && classMatches && visuallyMatches
    }
}
