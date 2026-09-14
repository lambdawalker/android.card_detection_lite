package com.apexfission.android.carddetectionlite.domain.tflite.detector.card.engine

import android.graphics.Bitmap
import android.os.SystemClock
import androidx.camera.core.ImageProxy
import com.apexfission.android.carddetectionlite.domain.tflite.detector.card.selection.CardCandidateSelector
import com.apexfission.android.carddetectionlite.domain.tflite.detector.card.tracking.CardLockStateMachine
import com.apexfission.android.carddetectionlite.domain.tflite.detector.card.tracking.TemporalConsistencyChecker
import com.apexfission.android.carddetectionlite.domain.tflite.detector.yolo.engine.Detector
import com.apexfission.android.carddetectionlite.domain.tflite.filters.AspectRatioValidator
import com.apexfission.android.carddetectionlite.domain.tflite.filters.CardValidator
import com.apexfission.android.carddetectionlite.domain.tflite.filters.MarginValidator
import com.apexfission.android.carddetectionlite.domain.tflite.image.toUprightBitmap
import com.apexfission.android.carddetectionlite.domain.tflite.model.CardDetection

internal class DefaultCardDetector internal constructor(
    private val yoloDetector: Detector,
    private val candidateSelector: CardCandidateSelector,
    private val stateMachine: CardLockStateMachine
) : CardDetector {

    constructor(
        yoloDetector: Detector,
        cardValidators: List<CardValidator> = listOf(
            AspectRatioValidator(),
            MarginValidator()
        ),
        cardClasses: Set<Int>,
        lockOnThreshold: Int = 5,
        memoryDetectionTimeLimit: Long = 1000L,
        validateClassIdInLockOnProcess: Boolean = true,
        noDetectionCountLimit: Int = 8,
        differenceHashDistanceLimit: Int = 25,
        allowTemporalDrift: Boolean = true
    ) : this(
        yoloDetector = yoloDetector,
        candidateSelector = CardCandidateSelector(
            cardClasses = cardClasses,
            cardValidators = cardValidators,
            validateClassIdInLockOnProcess = validateClassIdInLockOnProcess
        ),
        stateMachine = CardLockStateMachine(
            lockOnThreshold = lockOnThreshold,
            memoryDetectionTimeLimit = memoryDetectionTimeLimit,
            noDetectionCountLimit = noDetectionCountLimit,
            allowTemporalDrift = allowTemporalDrift,
            consistencyChecker = TemporalConsistencyChecker(
                validateClassIdInLockOnProcess = validateClassIdInLockOnProcess,
                differenceHashDistanceLimit = differenceHashDistanceLimit
            )
        )
    )

    override var enabled: Boolean
        get() = yoloDetector.enabled
        set(value) {
            yoloDetector.enabled = value
        }

    override fun track(imageProxy: ImageProxy): CardDetection? {
        val bitmap = imageProxy.toUprightBitmap()

        return try {
            track(bitmap)
        } finally {
            bitmap.recycle()
        }
    }

    override fun track(bitmap: Bitmap): CardDetection? {
        val result = yoloDetector.detect(bitmap)
        val currentTime = SystemClock.elapsedRealtime()

        val candidate = candidateSelector.selectCandidate(
            detections = result,
            previousDetection = stateMachine.previousCardDetection,
            bitmap = bitmap
        )

        if (candidate == null) {
            stateMachine.handleMissingDetection()
            return null
        }

        return stateMachine.processDetection(
            card = candidate,
            bitmap = bitmap,
            currentTimeMs = currentTime
        )
    }

    override fun close() {
        yoloDetector.close()
    }
}
