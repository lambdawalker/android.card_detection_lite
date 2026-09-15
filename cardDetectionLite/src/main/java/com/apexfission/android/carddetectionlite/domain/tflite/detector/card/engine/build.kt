package com.apexfission.android.carddetectionlite.domain.tflite.detector.card.engine

import com.apexfission.android.carddetectionlite.domain.tflite.detector.tflite.engine.EngineThreadDispatcher
import com.apexfission.android.carddetectionlite.domain.tflite.detector.yolo.engine.Detector
import com.apexfission.android.carddetectionlite.domain.tflite.filters.AspectRatioValidator
import com.apexfission.android.carddetectionlite.domain.tflite.filters.CardValidator
import com.apexfission.android.carddetectionlite.domain.tflite.filters.MarginValidator

fun buildCardDetector(
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
    allowTemporalDrift: Boolean = true,
    hashBasedSearch: Int = 3,
    sharedDispatcher: EngineThreadDispatcher? = null
): CardDetector = buildThreadConfinedCardDetector(
    yoloDetector = yoloDetector,
    cardValidators = cardValidators,
    cardClasses = cardClasses,
    lockOnThreshold = lockOnThreshold,
    memoryDetectionTimeLimit = memoryDetectionTimeLimit,
    validateClassIdInLockOnProcess = validateClassIdInLockOnProcess,
    noDetectionCountLimit = noDetectionCountLimit,
    differenceHashDistanceLimit = differenceHashDistanceLimit,
    allowTemporalDrift = allowTemporalDrift,
    hashBasedSearch = hashBasedSearch,
    sharedDispatcher = sharedDispatcher
)

fun buildThreadConfinedCardDetector(
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
    allowTemporalDrift: Boolean = true,
    hashBasedSearch: Int = 3,
    sharedDispatcher: EngineThreadDispatcher? = null
): CardDetector {
    return ThreadConfinedCardDetector(sharedDispatcher) {
        DefaultCardDetector(
            yoloDetector = yoloDetector,
            cardValidators = cardValidators,
            cardClasses = cardClasses,
            lockOnThreshold = lockOnThreshold,
            memoryDetectionTimeLimit = memoryDetectionTimeLimit,
            validateClassIdInLockOnProcess = validateClassIdInLockOnProcess,
            noDetectionCountLimit = noDetectionCountLimit,
            differenceHashDistanceLimit = differenceHashDistanceLimit,
            allowTemporalDrift = allowTemporalDrift,
            hashBasedSearch = hashBasedSearch
        )
    }
}
