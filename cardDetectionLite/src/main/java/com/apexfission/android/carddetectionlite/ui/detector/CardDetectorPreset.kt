package com.apexfission.android.carddetectionlite.ui.detector

import androidx.compose.runtime.Immutable
import com.apexfission.android.carddetectionlite.domain.tflite.detector.InputShape

/**
 * Configuration preset for tuning card detection pipeline accuracy, performance, and power consumption.
 *
 * @property scoreThreshold Minimum confidence score (0.0 to 1.0) required for candidate detections.
 * @property iouThreshold Intersection-over-Union threshold for Non-Max Suppression.
 * @property lockOnThreshold Number of consecutive consistent frames required before confirming lock-on.
 * @property noDetectionCountLimit Maximum number of missing detection frames allowed before tracking resets.
 * @property memoryDetectionTimeLimit Maximum elapsed time in milliseconds that tracking state may survive without a valid detection.
 * @property validateClassIdInLockOnProcess Whether a class-ID change should start a new candidate.
 * @property differenceHashDistanceLimit Maximum Hamming distance between two 64-bit dHashes for them to be considered visually similar.
 * @property allowTemporalDrift When true, each frame is compared to the immediately previous frame.
 * @property inferenceIntervalMs Minimum time interval in milliseconds between consecutive model inferences.
 * @property useGpu Whether to attempt GPU acceleration for TFLite inference.
 * @property imageMode Preprocessing strategy for input cropping ([InputShape]).
 * @property numThreads CPU thread configuration using [NumThreads].
 */
@Immutable
data class CardDetectorPreset(
    val scoreThreshold: Float,
    val iouThreshold: Float = 0.45f,
    val lockOnThreshold: Int,
    val noDetectionCountLimit: Int,
    val memoryDetectionTimeLimit: Long = 1000L,
    val validateClassIdInLockOnProcess: Boolean = true,
    val differenceHashDistanceLimit: Int = 25,
    val allowTemporalDrift: Boolean = true,
    val inferenceIntervalMs: Long,
    val useGpu: Boolean,
    val imageMode: InputShape,
    val numThreads: NumThreads
) {
    /**
     * Returns a copy of this [CardDetectorPreset] with the specified properties modified.
     */
    fun change(
        scoreThreshold: Float = this.scoreThreshold,
        iouThreshold: Float = this.iouThreshold,
        lockOnThreshold: Int = this.lockOnThreshold,
        noDetectionCountLimit: Int = this.noDetectionCountLimit,
        memoryDetectionTimeLimit: Long = this.memoryDetectionTimeLimit,
        validateClassIdInLockOnProcess: Boolean = this.validateClassIdInLockOnProcess,
        differenceHashDistanceLimit: Int = this.differenceHashDistanceLimit,
        allowTemporalDrift: Boolean = this.allowTemporalDrift,
        inferenceIntervalMs: Long = this.inferenceIntervalMs,
        useGpu: Boolean = this.useGpu,
        imageMode: InputShape = this.imageMode,
        numThreads: NumThreads = this.numThreads
    ): CardDetectorPreset = copy(
        scoreThreshold = scoreThreshold,
        iouThreshold = iouThreshold,
        lockOnThreshold = lockOnThreshold,
        noDetectionCountLimit = noDetectionCountLimit,
        memoryDetectionTimeLimit = memoryDetectionTimeLimit,
        validateClassIdInLockOnProcess = validateClassIdInLockOnProcess,
        differenceHashDistanceLimit = differenceHashDistanceLimit,
        allowTemporalDrift = allowTemporalDrift,
        inferenceIntervalMs = inferenceIntervalMs,
        useGpu = useGpu,
        imageMode = imageMode,
        numThreads = numThreads
    )

    companion object {
        /**
         * Preset optimized for high detection accuracy.
         * Uses a lower score threshold (0.35), full frame analysis ([InputShape.FullImage]),
         * and a higher lock-on threshold (6).
         */
        val HighAccuracy = CardDetectorPreset(
            scoreThreshold = 0.80f,
            iouThreshold = 0.45f,
            lockOnThreshold = 6,
            noDetectionCountLimit = 10,
            inferenceIntervalMs = 33L,
            useGpu = true,
            imageMode = InputShape.FullImage,
            numThreads = NumThreads.Default
        )

        /**
         * Preset optimized for real-time responsiveness and high frame rates (30 fps).
         * Uses GPU delegate, square crop preprocessing, and confidence score threshold 0.65.
         */
        val HighPerformance = CardDetectorPreset(
            scoreThreshold = 0.50f,
            iouThreshold = 0.45f,
            lockOnThreshold = 4,
            noDetectionCountLimit = 8,
            inferenceIntervalMs = 33L,
            useGpu = true,
            imageMode = InputShape.SquareCrop,
            numThreads = NumThreads.Default
        )

        /**
         * Preset optimized for low battery and CPU/GPU power consumption.
         * Throttles inference to 10 fps (100ms interval), disables GPU delegate, limits CPU to 2 threads.
         */
        val BatterySaver = CardDetectorPreset(
            scoreThreshold = 0.50f,
            iouThreshold = 0.45f,
            lockOnThreshold = 4,
            noDetectionCountLimit = 6,
            inferenceIntervalMs = 100L,
            useGpu = false,
            imageMode = InputShape.SquareCrop,
            numThreads = NumThreads.CustomCount(2)
        )
    }
}
