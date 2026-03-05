package com.apexfission.android.carddetectionlite.domain.tflite

import android.content.Context
import android.graphics.Bitmap
import androidx.camera.core.ImageProxy
import com.apexfission.android.carddetectionlite.domain.tflite.data.DetCutout
import com.apexfission.android.carddetectionlite.domain.tflite.data.RawDet
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

    fun detectCutouts(imageProxy: ImageProxy, maxCutouts: Int = 5): List<DetCutout> {
        if (!enabled) return emptyList()

        val bitmap = imageProxy.toUprightBitmap()
        val lb = ImageProcessor.letterboxToSquareReusable(bitmap, interpreter.inputImageWidth)

        val output = interpreter.runInference(lb.bitmap)

        val rawDetections = postProcessor.process(
            output = output,
            width = bitmap.width,
            height = bitmap.height,
            lbScale = lb.scale,
            padX = lb.padX,
            padY = lb.padY
        ).sortedByDescending { it.confidence }

        val filteredDetections = detectionFilters.fold(rawDetections) { filtered, filter ->
            filter.filter(filtered, bitmap.width, bitmap.height)
        }.take(maxCutouts)

        return filteredDetections.map {
            cropDet(bitmap, it, 0)
        }
    }

    @Synchronized
    override fun close() {
        if (isClosed) return
        isClosed = true
        interpreter.close()
        ImageProcessor.cleanUp()
    }
}