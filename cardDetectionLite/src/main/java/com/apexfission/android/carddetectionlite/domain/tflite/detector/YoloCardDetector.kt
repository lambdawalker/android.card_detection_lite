package com.apexfission.android.carddetectionlite.domain.tflite.detector

import android.graphics.Rect
import android.os.SystemClock
import androidx.camera.core.ImageProxy

import com.apexfission.android.carddetectionlite.domain.tflite.filters.CardValidator
import com.apexfission.android.carddetectionlite.domain.tflite.model.CardDetection
import java.io.Closeable

typealias onLockOnProgressCallBack = (progress: Float, candidate: CardDetection?) -> Unit

val emptyOnLockOnProgress: onLockOnProgressCallBack = { _, _ -> }

class YoloCardDetector(
    private val yoloDetector: YoloDetector,
    private val cardValidators: List<CardValidator>,
    private val cardClasses: List<Int>,
    private val onLockOnProgress: onLockOnProgressCallBack = emptyOnLockOnProgress
) : Closeable {
    private var previousHash: ULong = 0u
    private var similarityCount = 0
    private var isSent: Boolean = false
    private var lastDetectionTime: Long = 0L
    private var cardId = 0
    private val stabilityThreshold = 5

    fun extractCard(imageProxy: ImageProxy): CardDetection? {
        val result = yoloDetector.extractFeatures(imageProxy)
        val currentTime = SystemClock.elapsedRealtime()

        if (currentTime - lastDetectionTime > 1000 && lastDetectionTime != 0L) {
            resetTrackingState()
        }

        val card = result.extractedFeatures.firstOrNull {
            it.classId in cardClasses && cardValidators.all { filter ->
                filter.isValid(it, result.imageWidth, result.imageHeight)
            }
        }

        if (card == null) {
            onLockOnProgress(0f, null)
            return null
        }

        lastDetectionTime = currentTime

        val currentHash = card.objectBitmap.generateDHash(16)

        val isSimilar = isVisuallySimilar(previousHash, currentHash, 15)

        if (isSimilar) {
            similarityCount += 1
        } else {
            similarityCount = 0
            isSent = false
        }

        previousHash = currentHash
        val progress = (similarityCount.toFloat() / stabilityThreshold).coerceAtMost(1f)

        val otherElements = result.extractedFeatures.filter { region ->
            val centerX = (region.coordinates.left + region.coordinates.right) / 2
            val centerY = (region.coordinates.top + region.coordinates.bottom) / 2

            region.classId !in cardClasses &&
                card.coordinates.contains(centerX, centerY)
        }

        if (similarityCount > stabilityThreshold && !isSent) {
            isSent = true
            cardId++
        }

        val cardDetection = CardDetection(
            id = cardId,
            card = card,
            features = otherElements,
            contextSize = Rect(0, 0, result.imageWidth, result.imageHeight)
        )

        onLockOnProgress(progress, cardDetection)
        return cardDetection
    }

    private fun resetTrackingState() {
        similarityCount = 0
        isSent = false
        previousHash = 0u
    }

    var enabled: Boolean
        get() = yoloDetector.enabled
        set(value) {
            yoloDetector.enabled = value
        }

    override fun close() = yoloDetector.close()
}