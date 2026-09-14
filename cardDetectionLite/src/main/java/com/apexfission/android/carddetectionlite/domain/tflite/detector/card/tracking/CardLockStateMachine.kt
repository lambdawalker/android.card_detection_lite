package com.apexfission.android.carddetectionlite.domain.tflite.detector.card.tracking

import android.graphics.Bitmap
import com.apexfission.android.carddetectionlite.domain.tflite.image.generateDHashFromRegion
import com.apexfission.android.carddetectionlite.domain.tflite.model.CardDetection
import com.apexfission.android.carddetectionlite.domain.tflite.model.Detection
import com.apexfission.android.carddetectionlite.domain.tflite.model.Feature
import com.apexfission.android.carddetectionlite.domain.tflite.model.LockingStatus

/**
 * Manages mutable state transitions and lock progression for tracked card candidates across frames.
 */
internal class CardLockStateMachine(
    private val lockOnThreshold: Int = 5,
    private val memoryDetectionTimeLimit: Long = 1000L,
    private val noDetectionCountLimit: Int = 8,
    private val allowTemporalDrift: Boolean = true,
    private val consistencyChecker: TemporalConsistencyChecker = TemporalConsistencyChecker()
) {

    private var comparisonHash: ULong? = null
    private var candidateConsistencyCount: Int = 0
    private var isSent: Boolean = false
    private var lastDetectionTime: Long = 0L
    private var cardIdCount: Long = 0
    var previousCardDetection: Detection? = null
        private set
    private var noDetectionCount: Int = 0

    init {
        require(lockOnThreshold > 0) {
            "lockOnThreshold must be greater than 0"
        }

        require(memoryDetectionTimeLimit >= 0L) {
            "memoryDetectionTimeLimit must be >= 0"
        }

        require(noDetectionCountLimit > 0) {
            "noDetectionCountLimit must be greater than 0"
        }
    }

    /**
     * Processes a valid [card] candidate from [bitmap] at [currentTimeMs], returning the
     * updated [CardDetection].
     */
    fun processDetection(
        card: Detection,
        bitmap: Bitmap,
        currentTimeMs: Long
    ): CardDetection {
        if (
            memoryDetectionTimeLimit > 0L &&
            lastDetectionTime != 0L &&
            currentTimeMs - lastDetectionTime > memoryDetectionTimeLimit
        ) {
            resetTrackingState()
        }

        noDetectionCount = 0
        lastDetectionTime = currentTimeMs

        val currentHash = bitmap.generateDHashFromRegion(card.box)

        val continuesCandidate = consistencyChecker.isConsistent(
            candidate = card,
            currentHash = currentHash,
            previousDetection = previousCardDetection,
            previousComparisonHash = comparisonHash
        )

        if (continuesCandidate) {
            if (candidateConsistencyCount < lockOnThreshold) {
                candidateConsistencyCount++
            }

            if (allowTemporalDrift) {
                comparisonHash = currentHash
            }
        } else {
            candidateConsistencyCount = 1
            isSent = false
            comparisonHash = currentHash
        }

        previousCardDetection = card

        val lockingStatus = when {
            candidateConsistencyCount >= lockOnThreshold && !isSent -> {
                isSent = true
                cardIdCount++
                LockingStatus.NewCard
            }

            candidateConsistencyCount >= lockOnThreshold -> {
                LockingStatus.CardLocked
            }

            else -> {
                LockingStatus.LockingCard
            }
        }

        val cardId = when (lockingStatus) {
            LockingStatus.NewCard,
            LockingStatus.CardLocked -> cardIdCount
            else -> null
        }

        val lockOnProgress = (candidateConsistencyCount.toFloat() / lockOnThreshold).coerceIn(0f, 1f)

        return CardDetection(
            id = cardId,
            lockingStatus = lockingStatus,
            card = Feature(
                card.box,
                card.confidence,
                card.classId
            ),
            features = emptyList(),
            lockOnProgress = lockOnProgress
        )
    }

    /**
     * Invoked when a frame yields no valid card candidate.
     */
    fun handleMissingDetection() {
        noDetectionCount++

        if (noDetectionCount >= noDetectionCountLimit) {
            resetTrackingState()
        }
    }

    /**
     * Resets all tracking and locking state.
     */
    fun resetTrackingState() {
        previousCardDetection = null
        comparisonHash = null
        candidateConsistencyCount = 0
        noDetectionCount = 0
        isSent = false
        lastDetectionTime = 0L
    }
}
