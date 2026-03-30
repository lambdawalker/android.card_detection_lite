package com.apexfission.android.carddetectionlite.domain.tflite.detector

import android.graphics.Rect
import android.os.SystemClock
import android.util.Log
import androidx.camera.core.ImageProxy
import com.apexfission.android.carddetectionlite.domain.tflite.filters.CardValidator
import com.apexfission.android.carddetectionlite.domain.tflite.image.generateDHash
import com.apexfission.android.carddetectionlite.domain.tflite.image.hammingDistanceTo
import com.apexfission.android.carddetectionlite.domain.tflite.image.isVisuallySimilar
import com.apexfission.android.carddetectionlite.domain.tflite.model.CardDetection
import java.io.Closeable

/**
 * A detector that uses a YOLO model to detect cards.
 *
 * @param yoloDetector The YOLO detector to use.
 * @param cardValidators The card validators to use. Filters applied to items included in the cardClasses.
 * @param cardClasses The card classes to detect.
 */
class YoloCardDetector(
    private val yoloDetector: YoloDetector,
    private val cardValidators: List<CardValidator>,
    private val cardClasses: List<Int>,
) : Closeable {
    private var previousHash: ULong = 0u
    private var similarityCount = 0
    private var isSent: Boolean = false
    private var lastDetectionTime: Long = 0L
    private var cardIdCount = 0
    private val stabilityThreshold = 5

    /**
     * Extracts a card from an ImageProxy and tracks its stability over time.
     *
     * This function analyzes the image to find a card that matches the specified `cardClasses` and passes all `cardValidators`.
     * It then tracks the detected card across consecutive executions to ensure it is a stable detection.
     *
     * The stability of the detection is determined using a perceptual hash.
     * This hash represents the visual features of the detected card. By comparing the hash of the card in the current frame
     * to the hash from the previous frame, the function can determine if the same card is being detected consistently.
     * This method is robust to minor changes in lighting, angle, and position.
     *
     * @param imageProxy The ImageProxy to extract the card from.
     * @return A [CardDetection] object if a stable card is found, otherwise null.
     *         The `CardDetection` object contains:
     *         - `id`: A unique identifier for the detected card instance.
     *         - `card`: The primary detected card feature.
     *         - `features`: A list of other detected objects located within the bounding box of the card.
     *         - `contextSize`: The dimensions of the image in which the detection occurred.
     *         - `lockOnProgress`: A float value between 0.0 and 1.0 indicating the detection's stability.
     *           A value of 1.0 means the card has been consistently detected across enough frames to be considered "locked on."
     *           This is useful for providing user feedback, such as filling a progress bar.
     */
    fun extractCard(imageProxy: ImageProxy): CardDetection? {
        val result = yoloDetector.extractFeatures(imageProxy)
        val currentTime = SystemClock.elapsedRealtime()

        if (currentTime - lastDetectionTime > 1000 && lastDetectionTime != 0L) {
            resetTrackingState()
        }

        val card = result.extractedFeatures.firstOrNull {
            it.classId in cardClasses && cardValidators.all { filter ->
                filter.isValid(
                    it,
                    result.contextWidth,
                    result.contextHeight,
                    result.originalWidth,
                    result.originalHeight
                )
            }
        }

        if (card == null) {
            return null
        }

        lastDetectionTime = currentTime

        val currentHash = card.objectBitmap.generateDHash(8)

        val isSimilar = isVisuallySimilar(previousHash, currentHash, 20)

        Log.d("HDLOG", "$isSimilar $previousHash $currentHash ${previousHash.hammingDistanceTo(currentHash)}")

        if (!isSimilar) {
            resetTrackingState()
        } else if (similarityCount <= stabilityThreshold) {
            similarityCount++
        }

        previousHash = currentHash

        val isNewDetection = if (similarityCount >= stabilityThreshold && !isSent) {
            isSent = true
            cardIdCount++
            true
        } else {
            false
        }

        val cardId = if (similarityCount >= stabilityThreshold) {
            cardIdCount
        } else {
            null
        }

        val lockOnProgress = (similarityCount.toFloat() / stabilityThreshold).coerceAtMost(1f)

        val otherElements = result.extractedFeatures.filter { region ->
            val centerX = (region.coordinates.left + region.coordinates.right) / 2
            val centerY = (region.coordinates.top + region.coordinates.bottom) / 2

            region.classId !in cardClasses && card.coordinates.contains(centerX, centerY)
        }


        val cardDetection = CardDetection(
            id = cardId,
            isNewDetection = isNewDetection,
            card = card,
            features = otherElements,
            contextSize = Rect(0, 0, result.contextWidth, result.contextHeight),
            lockOnProgress = lockOnProgress
        )

        return cardDetection
    }

    private fun resetTrackingState() {
        similarityCount = 0
        isSent = false
        previousHash = 0u
    }

    /**
     * Whether the detector is enabled.
     */
    var enabled: Boolean
        get() = yoloDetector.enabled
        set(value) {
            yoloDetector.enabled = value
        }

    /**
     * Closes the detector.
     */
    override fun close() = yoloDetector.close()
}