package com.apexfission.android.carddetectionlite.domain.tflite.detector

import android.util.Log
import androidx.camera.core.ImageProxy
import com.apexfission.android.carddetectionlite.domain.tflite.filters.CardValidator
import com.apexfission.android.carddetectionlite.domain.tflite.model.CardDetection
import java.io.Closeable

class YoloCardDetector(
    private val yoloDetector: YoloDetector,
    private val cardFilters: List<CardValidator>,
    private val cardClasses: List<Int>
) : Closeable {
    private var previous: ULong = 0u
    private var similarityCount = 0

    fun extractCard(imageProxy: ImageProxy): CardDetection? {
        val result = yoloDetector.extractFeatures(imageProxy)

        val card = result.extractedFeatures.firstOrNull {
            it.classId in cardClasses && cardFilters.all { filter ->
                filter.isValid(it, result.imageWidth, result.imageHeight)
            }
        } ?: return null

        val otherElements = result.extractedFeatures.filter { detection ->
            detection.classId !in cardClasses
        }

        val current = card.objectBitmap.generateDHash(16)

        val isSimilar = isVisuallySimilar(previous, current, 15)

        if (isSimilar) {
            similarityCount += 1
        } else {
            similarityCount = 0
        }

        if (similarityCount > 10) {
            similarityCount = 0
            Log.d("COMPX", "HERE")
        }

        previous = current

        return CardDetection(
            card,
            otherElements
        )
    }

    var enabled: Boolean
        get() = yoloDetector.enabled
        set(value) {
            yoloDetector.enabled = value
        }

    override fun close() {
        yoloDetector.close()
    }
}