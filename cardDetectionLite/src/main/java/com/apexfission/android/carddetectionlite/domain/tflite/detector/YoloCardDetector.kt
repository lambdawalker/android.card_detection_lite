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
 * A specialized detector that builds upon [YoloDetector] to provide higher-level logic
 * for card detection, including validation and temporal stability tracking.
 *
 * This class is responsible for:
 * 1.  Filtering raw detections to find objects that look like cards.
 * 2.  Applying a series of custom validation rules ([CardValidator]).
 * 3.  Tracking a detected card across multiple frames using perceptual hashing to ensure
 *     it is a stable and consistent object before confirming a "lock-on."
 *
 * @param yoloDetector The underlying [YoloDetector] instance that performs the raw object detection.
 * @param cardValidators A list of [CardValidator] implementations. A detected object is only
 *                       considered a valid card if it passes *all* of these validation checks.
 *                       This allows for flexible rules based on aspect ratio, size, position, etc.
 * @param cardClasses A list of integer class IDs from the TFLite model that represent cards.
 *                    Detections with a class ID not in this list will be ignored as primary targets
 *                    but can still be considered sub-features.
 * @param lockOnThreshold The number of consecutive frames a card must be detected and visually
 *                        similar before it is considered "locked on."
 */
class YoloCardDetector(
    private val yoloDetector: YoloDetector,
    private val cardValidators: List<CardValidator>,
    private val cardClasses: List<Int>,
    private val lockOnThreshold: Int = 5,
) : Closeable {
    private var previousHash: ULong = 0u
    private var similarityCount = 0
    private var isSent: Boolean = false
    private var lastDetectionTime: Long = 0L
    private var cardIdCount = 0

    /**
     * Analyzes an image frame to find and track a card.
     *
     * This is the core function of the class. It uses the underlying `yoloDetector` to get features,
     * then applies filtering and validation. It employs a temporal filtering mechanism using a
     * perceptual hash (`dHash`) to ensure a card is detected consistently across several
     * consecutive frames before triggering a "new detection" event.
     *
     * The `lockOnProgress` provides real-time feedback on this stability check.
     *
     * @param imageProxy The ImageProxy to extract the card from.
     * @return A [CardDetection] object if a stable card is found, otherwise null.
     */
    fun extractCard(imageProxy: ImageProxy): CardDetection? {
        val result = yoloDetector.extractFeatures(imageProxy)
        val currentTime = SystemClock.elapsedRealtime()

        // Reset tracking if no card has been seen for a while.
        if (currentTime - lastDetectionTime > 1000 && lastDetectionTime != 0L) {
            resetTrackingState()
        }

        // Find the first detected feature that is a card and passes all validators.
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
            resetTrackingState() // Also reset if a valid card disappears.
            return null
        }

        lastDetectionTime = currentTime

        // --- Temporal Stability Check using Perceptual Hashing ---
        val currentHash = card.objectBitmap.generateDHash(8)
        val isSimilar = isVisuallySimilar(previousHash, currentHash, 20)

        Log.d("HDLOG", "$isSimilar $previousHash $currentHash ${previousHash.hammingDistanceTo(currentHash)}")

        if (!isSimilar) {
            // If the card is visually different from the last one, reset the stability counter.
            resetTrackingState()
        } else if (similarityCount <= lockOnThreshold) {
            // If it's the same card, increment the counter.
            similarityCount++
        }

        previousHash = currentHash

        // --- State Calculation ---
        val isNewDetection = if (similarityCount >= lockOnThreshold && !isSent) {
            isSent = true // Ensure we only flag "new" once per lock-on.
            cardIdCount++
            true
        } else {
            false
        }

        val cardId = if (similarityCount >= lockOnThreshold) cardIdCount else null
        val lockOnProgress = (similarityCount.toFloat() / lockOnThreshold).coerceAtMost(1f)

        // Find other detected features that are inside the bounding box of the main card.
        val otherElements = result.extractedFeatures.filter { region ->
            val centerX = (region.coordinates.left + region.coordinates.right) / 2
            val centerY = (region.coordinates.top + region.coordinates.bottom) / 2
            region.classId !in cardClasses && card.coordinates.contains(centerX, centerY)
        }

        return CardDetection(
            id = cardId,
            isNewDetection = isNewDetection,
            card = card,
            features = otherElements,
            contextSize = Rect(0, 0, result.contextWidth, result.contextHeight),
            lockOnProgress = lockOnProgress
        )
    }

    /** Resets the state of the temporal tracking variables. */
    private fun resetTrackingState() {
        similarityCount = 0
        isSent = false
        previousHash = 0u
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
     * Releases resources held by the underlying [YoloDetector].
     */
    override fun close() = yoloDetector.close()
}
