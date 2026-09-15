package com.apexfission.android.carddetectionlite.domain.tflite.detector.card.tracking

import android.graphics.Bitmap
import android.os.SystemClock
import com.apexfission.android.carddetectionlite.domain.tflite.image.generateDHashFromRegion
import com.apexfission.android.carddetectionlite.domain.tflite.image.isVisuallySimilar
import com.apexfission.android.carddetectionlite.domain.tflite.model.CardDetection
import com.apexfission.android.carddetectionlite.domain.tflite.model.Detection
import com.apexfission.android.carddetectionlite.domain.tflite.model.DetectionSource
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
    private val hashBasedSearchFrameLimit: Int = 3,
    private val consistencyChecker: TemporalConsistencyChecker = TemporalConsistencyChecker(),
) {

    var previousHash: ULong? = null
        private set
    private var candidateConsistencyCount = 0f
    private var isSent: Boolean = false
    private var lastDetectionTime: Long = 0L
    private var cardIdCount: Long = 0

    var previousCardDetection: Detection? = null
        private set

    var hashDetectionCount: Int = 0
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
        card: Detection, bitmap: Bitmap, currentTimeMs: Long
    ): CardDetection {
        if (memoryDetectionTimeLimit > 0L && lastDetectionTime != 0L && currentTimeMs - lastDetectionTime > memoryDetectionTimeLimit) {
            resetTrackingState()
        }

        noDetectionCount = 0
        hashDetectionCount = 0
        lastDetectionTime = currentTimeMs

        val currentHash = bitmap.generateDHashFromRegion(card.box)


        val continuesCandidate = consistencyChecker.isConsistent(
            candidate = card, currentHash = currentHash, previousDetection = previousCardDetection, previousHash = previousHash
        )

        if (continuesCandidate) {

            if (candidateConsistencyCount < lockOnThreshold) {
                candidateConsistencyCount++
            }

            if (allowTemporalDrift) {
                previousHash = currentHash
            }
        } else {

            candidateConsistencyCount = 1f
            isSent = false
            previousHash = currentHash
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
            LockingStatus.NewCard, LockingStatus.CardLocked -> cardIdCount

            else -> null
        }

        val lockOnProgress = (candidateConsistencyCount / lockOnThreshold).coerceIn(0f, 1f)

        return CardDetection(
            id = cardId, lockingStatus = lockingStatus, card = Feature(
                card.box, card.confidence, card.classId
            ), features = emptyList(), lockOnProgress = lockOnProgress, detectionSource = DetectionSource.Yolo
        )
    }

    /**
     * Continues tracking an existing card candidate without re-evaluating candidate selection.
     */
    fun giveContinuation(
        card: Detection,
        bitmap: Bitmap,
        currentTimeMs: Long = SystemClock.elapsedRealtime(),
    ): CardDetection? {
        if (memoryDetectionTimeLimit > 0L && lastDetectionTime != 0L && currentTimeMs - lastDetectionTime > memoryDetectionTimeLimit) {
            resetTrackingState()
        }

        noDetectionCount = 0
        hashDetectionCount++
        lastDetectionTime = currentTimeMs

        if (candidateConsistencyCount < lockOnThreshold) {
            //hash detection carries less validity
            candidateConsistencyCount += 1f / hashBasedSearchFrameLimit
        }

        val ghostDHash = bitmap.generateDHashFromRegion(card.box)
        val isConsistent = consistencyChecker.isConsistent(ghostDHash, previousHash)

        if (!isConsistent) {
            return null
        }

        val lockingStatus = when {
            candidateConsistencyCount >= lockOnThreshold.toFloat() && !isSent -> {
                //avoid creating a new card state for a hash-based search
                candidateConsistencyCount = lockOnThreshold - 1f
                LockingStatus.LockingCard
            }

            candidateConsistencyCount >= lockOnThreshold -> {
                LockingStatus.CardLocked
            }

            else -> {
                LockingStatus.LockingCard
            }
        }

        val cardId = when (lockingStatus) {
            LockingStatus.CardLocked -> cardIdCount
            else -> null
        }

        val lockOnProgress = (candidateConsistencyCount / lockOnThreshold).coerceIn(0f, 1f)

        return CardDetection(
            id = cardId, lockingStatus = lockingStatus, card = Feature(
                card.box, card.confidence, card.classId
            ), features = emptyList(), lockOnProgress = lockOnProgress, detectionSource = DetectionSource.Hash
        )
    }

    fun buildCandidateFromLastKnownCoordinates(
        bitmap: Bitmap, differenceHashDistanceLimit: Int
    ): Detection? {
        val previousCardDetection = previousCardDetection ?: return null
        val previousHash = previousHash ?: return null

        val box = previousCardDetection.box
        val left = box.x.toInt().coerceIn(0, bitmap.width)
        val top = box.y.toInt().coerceIn(0, bitmap.height)
        val right = box.x2.toInt().coerceIn(0, bitmap.width)
        val bottom = box.y2.toInt().coerceIn(0, bitmap.height)

        if (right <= left || bottom <= top) {
            return null
        }

        val currentHash = try {
            bitmap.generateDHashFromRegion(box)
        } catch (e: Exception) {
            return null
        }

        val visuallyMatches = isVisuallySimilar(
            previousHash, currentHash, differenceHashDistanceLimit
        )

        return if (visuallyMatches) {
            previousCardDetection.copy()
        } else {
            null
        }
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
        previousHash = null
        candidateConsistencyCount = 0f
        noDetectionCount = 0
        hashDetectionCount = 0
        isSent = false
        lastDetectionTime = 0L
    }

    fun resetHashTrackingState() {
        hashDetectionCount = 0
    }
}
