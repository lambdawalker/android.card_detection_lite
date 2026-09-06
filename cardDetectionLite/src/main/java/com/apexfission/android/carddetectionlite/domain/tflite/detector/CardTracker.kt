package com.apexfission.android.carddetectionlite.domain.tflite.detector

import android.graphics.Bitmap
import android.os.SystemClock
import android.util.Log
import androidx.camera.core.ImageProxy
import com.apexfission.android.carddetectionlite.domain.tflite.filters.AspectRatioValidator
import com.apexfission.android.carddetectionlite.domain.tflite.filters.CardValidator
import com.apexfission.android.carddetectionlite.domain.tflite.filters.MarginValidator
import com.apexfission.android.carddetectionlite.domain.tflite.image.crop
import com.apexfission.android.carddetectionlite.domain.tflite.image.generateDHashFromRegion
import com.apexfission.android.carddetectionlite.domain.tflite.image.isVisuallySimilar
import com.apexfission.android.carddetectionlite.domain.tflite.image.toUprightBitmap
import com.apexfission.android.carddetectionlite.domain.tflite.model.CardDetection2
import com.apexfission.android.carddetectionlite.domain.tflite.model.Detection2
import com.apexfission.android.carddetectionlite.domain.tflite.model.Feature
import com.apexfission.android.carddetectionlite.domain.tflite.model.LockingStatus
import java.io.Closeable

/**
 * Higher-level card detector built on top of [YoloDetector].
 *
 * Responsibilities:
 * - Runs YOLO detection.
 * - Filters detections to supported card classes.
 * - Applies card validators.
 * - Attempts to maintain spatial continuity between detections.
 * - Checks class and visual consistency.
 * - Requires a configurable number of consistent frames before locking.
 * - Tolerates a limited number of missed detections.
 *
 * @param yoloDetector Underlying YOLO detector.
 * @param cardValidators Validators that a card candidate must pass.
 * @param cardClasses YOLO class IDs that represent cards.
 * @param lockOnThreshold Number of consistent detected frames required before lock-on.
 * @param memoryDetectionTimeLimit Maximum elapsed time in milliseconds that tracking state
 * may survive without a valid detection. A value of 0 disables the time-based reset.
 * @param validateClassIdInLockOnProcess Whether a class-ID change should start a new candidate.
 * @param noDetectionCountLimit Number of consecutive missing detections allowed before reset.
 * @param differenceHashDistanceLimit Maximum Hamming distance between two 64-bit dHashes
 * for them to be considered visually similar.
 * @param allowTemporalDrift When true, each frame is compared to the immediately previous
 * frame. When false, frames are compared against the first frame of the current candidate.
 */
class CardTracker(
    private val yoloDetector: YoloDetector,
    private val cardValidators: List<CardValidator> = listOf(
        AspectRatioValidator(),
        MarginValidator()
    ),
    private val cardClasses: Set<Int>,
    private val lockOnThreshold: Int = 5,
    private val memoryDetectionTimeLimit: Long = 1000L,
    private val validateClassIdInLockOnProcess: Boolean = true,
    private val noDetectionCountLimit: Int = 8,
    private val differenceHashDistanceLimit: Int = 25,
    private val allowTemporalDrift: Boolean = true
) : Closeable {

    /**
     * Hash used for visual comparison.
     *
     * When [allowTemporalDrift] is true, this represents the previous frame.
     *
     * When [allowTemporalDrift] is false, this represents the first frame
     * of the current candidate sequence.
     */
    private var comparisonHash: ULong? = null

    /**
     * Number of consecutive frames belonging to the current candidate.
     */
    private var candidateConsistencyCount: Int = 0

    /**
     * True once the current candidate has emitted [LockingStatus.NewCard].
     */
    private var isSent: Boolean = false

    /**
     * elapsedRealtime() timestamp of the most recent valid card detection.
     */
    private var lastDetectionTime: Long = 0L

    /**
     * Monotonically increasing identifier assigned whenever a new card locks.
     */
    private var cardIdCount: Long = 0

    /**
     * Most recent card detection.
     *
     * Used by validators and spatial candidate matching.
     */
    private var previousCardDetection: Detection2? = null

    /**
     * Number of consecutive frames where no valid card was detected.
     */
    private var noDetectionCount: Int = 0

    init {
        require(cardClasses.isNotEmpty()) {
            "cardClasses must not be empty"
        }

        require(lockOnThreshold > 0) {
            "lockOnThreshold must be greater than 0"
        }

        require(memoryDetectionTimeLimit >= 0L) {
            "memoryDetectionTimeLimit must be >= 0"
        }

        require(noDetectionCountLimit > 0) {
            "noDetectionCountLimit must be greater than 0"
        }

        require(differenceHashDistanceLimit in 0..64) {
            "differenceHashDistanceLimit must be between 0 and 64"
        }
    }

    /**
     * Tracks a card from an [ImageProxy].
     *
     * The caller remains responsible for closing [imageProxy].
     */
    fun track(imageProxy: ImageProxy): CardDetection2? {
        val bitmap = imageProxy.toUprightBitmap()
        val result = yoloDetector.detect(bitmap)

        return processDetections(
            result = result,
            bitmap = bitmap
        )
    }

    /**
     * Tracks a card from an already-created upright [Bitmap].
     */
    fun track(bitmap: Bitmap): CardDetection2? {
        val result = yoloDetector.detect(bitmap)

        return processDetections(
            result = result,
            bitmap = bitmap
        )
    }

    private fun processDetections(
        result: List<Detection2>,
        bitmap: Bitmap
    ): CardDetection2? {
        val currentTime = SystemClock.elapsedRealtime()
        Log.d("CardTracker", "Raw results: ${result.size} ${cardValidators.size}")

        if (
            memoryDetectionTimeLimit > 0L &&
            lastDetectionTime != 0L &&
            currentTime - lastDetectionTime > memoryDetectionTimeLimit
        ) {
            resetTrackingState()
        }

        val candidates = result.filter { detection ->
            detection.classId in cardClasses &&
                cardValidators.all { validator ->
                    validator.isValid(
                        detection,
                        previousCardDetection,
                        bitmap
                    )
                }
        }

        Log.d("CardTracker", "Filtered results: ${candidates.size}")

        val card = selectCandidate(candidates)

        Log.d("CardTracker", "Selected card: $card")

        if (card == null) {
            handleMissingDetection()
            return null
        }

        noDetectionCount = 0
        lastDetectionTime = currentTime

        // Hash the detected region directly without allocating a cropped bitmap.
        val currentHash = bitmap.generateDHashFromRegion(card.box)

        val previousDetection = previousCardDetection
        val previousComparisonHash = comparisonHash

        val hasPreviousCandidate =
            previousDetection != null &&
                previousComparisonHash != null

        val classMatches =
            !validateClassIdInLockOnProcess ||
                previousDetection == null ||
                card.classId == previousDetection.classId

        val visuallyMatches =
            previousComparisonHash?.let { hash ->
                isVisuallySimilar(
                    hash,
                    currentHash,
                    differenceHashDistanceLimit
                )
            } ?: false

        val continuesCandidate =
            hasPreviousCandidate &&
                classMatches &&
                visuallyMatches

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

        val lockOnProgress =
            (candidateConsistencyCount.toFloat() / lockOnThreshold)
                .coerceIn(0f, 1f)

        return CardDetection2(
            id = cardId,
            lockingStatus = lockingStatus,
            card = Feature(
                card.box,
                card.confidence,
                card.classId,
                bitmap.crop(card.box)
            ),
            features = emptyList(),
            lockOnProgress = lockOnProgress
        )
    }


    private fun selectCandidate(
        candidates: List<Detection2>
    ): Detection2? {
        if (candidates.isEmpty()) {
            return null
        }

        val previousDetection = previousCardDetection
            ?: return candidates.maxByOrNull { it.confidence }

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

    private fun handleMissingDetection() {
        noDetectionCount++

        if (noDetectionCount >= noDetectionCountLimit) {
            resetTrackingState()
        }
    }

    private fun resetTrackingState() {
        previousCardDetection = null
        comparisonHash = null

        candidateConsistencyCount = 0
        noDetectionCount = 0

        isSent = false
        lastDetectionTime = 0L
    }

    /**
     * Controls the enabled state of the underlying [YoloDetector].
     */
    var enabled: Boolean
        get() = yoloDetector.enabled
        set(value) {
            yoloDetector.enabled = value
        }

    /**
     * Releases resources held by the underlying detector.
     */
    override fun close() {
        yoloDetector.close()
    }
}