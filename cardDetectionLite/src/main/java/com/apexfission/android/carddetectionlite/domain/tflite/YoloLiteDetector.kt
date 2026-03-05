package com.apexfission.android.carddetectionlite.domain.tflite

import android.content.Context
import android.graphics.Bitmap
import androidx.camera.core.ImageProxy
import com.apexfission.android.carddetectionlite.domain.tflite.data.Detection
import com.apexfission.android.carddetectionlite.domain.tflite.data.RawDet
import com.apexfission.android.carddetectionlite.domain.tflite.data.buildDetection
import com.apexfission.android.carddetectionlite.domain.tflite.filters.DetectionFilter
import java.io.Closeable

class YoloLiteDetector(
    context: Context,
    modelPath: String,
    scoreThreshold: Float,
    iouThreshold: Float,
    useGpu: Boolean,
    private val detectionFilters: List<DetectionFilter> = emptyList(),
    maxNmsCandidates: Int = 300,
    numThreads: Int? = null,
) : Closeable {

    var enabled: Boolean = true
    private var isClosed = false

    private val interpreter = TfliteInterpreter(context, modelPath, useGpu, numThreads)
    private val postProcessor = YoloPostProcessor(
        interpreter.outLayout,
        interpreter.outBoxes,
        interpreter.outAttrs,
        interpreter.numClasses,
        interpreter.inputImageWidth,
        scoreThreshold,
        iouThreshold,
        maxNmsCandidates
    )

    fun detect(bitmap: Bitmap): List<RawDet> {
        if (!enabled || isClosed) return emptyList()

        val lb = ImageProcessor.letterboxToSquareReusable(bitmap, interpreter.inputImageWidth)
        val output = interpreter.runInference(lb.bitmap)

        return postProcessor.process(
            output = output,
            width = bitmap.width,
            height = bitmap.height,
            lbScale = lb.scale,
            padX = lb.padX,
            padY = lb.padY
        )
    }

    fun detect(imageProxy: ImageProxy): List<RawDet> {
        if (!enabled || isClosed) return emptyList()
        val bitmap = imageProxy.toUprightBitmap()
        return detect(bitmap)
    }

    fun detectCutouts(imageProxy: ImageProxy, maxCutouts: Int = 5): List<Detection> {
        if (!enabled || isClosed) return emptyList()

        val bitmap = imageProxy.toUprightBitmap()
        val rawDetections = detect(bitmap)

        val detections = rawDetections.take(maxCutouts).map {
            buildDetection(bitmap, it, 0)
        }.filter {
            detectionFilters.all { filter ->
                filter.filter(it, bitmap.width, bitmap.height)
            }
        }

        return detections
    }

    @Synchronized
    override fun close() {
        if (isClosed) return
        isClosed = true
        interpreter.close()
        ImageProcessor.cleanUp()
    }
}