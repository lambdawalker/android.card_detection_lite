package com.apexfission.android.carddetectionlite.domain.tflite.detector

import androidx.camera.core.ImageProxy
import com.apexfission.android.carddetectionlite.domain.tflite.filters.DetectionFilter
import com.apexfission.android.carddetectionlite.domain.tflite.model.Detection
import java.io.Closeable

class YoloCardDetector(
    private val yoloDetector: YoloDetector,
    private val detectionFilters: List<DetectionFilter>,
    private val cardClasses: List<Int>
) : Closeable {
    fun detect(imageProxy: ImageProxy): List<Detection> {
        val result = yoloDetector.detectCutouts(imageProxy)

        return result.detections.filter { detection ->
            detectionFilters.all { filter ->
                detection.classId !in cardClasses
                    || filter.filter(detection, result.imageWidth, result.imageHeight)
            }
        }
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