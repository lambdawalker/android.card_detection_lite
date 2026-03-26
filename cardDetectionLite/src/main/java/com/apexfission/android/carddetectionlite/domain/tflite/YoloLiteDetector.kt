package com.apexfission.android.carddetectionlite.domain.tflite

import android.content.Context
import android.graphics.Bitmap
import androidx.camera.core.ImageProxy
import androidx.compose.ui.unit.IntSize
import com.apexfission.android.carddetectionlite.domain.tflite.data.Detection
import com.apexfission.android.carddetectionlite.domain.tflite.data.RawDet
import com.apexfission.android.carddetectionlite.domain.tflite.data.buildDetection
import com.apexfission.android.carddetectionlite.domain.tflite.filters.DetectionFilter
import java.io.Closeable
import kotlinx.coroutines.flow.MutableStateFlow

class YoloLiteDetector(
    context: Context,
    modelPath: String,
    scoreThreshold: Float,
    iouThreshold: Float,
    useGpu: Boolean,
    private val detectionFilters: List<DetectionFilter> = emptyList(),
    maxNmsCandidates: Int = 300,
    numThreads: Int? = null,
    private val canvasSize: MutableStateFlow<IntSize>,
    private val imageMode: InputShape,
) : Closeable {

    enum class InputShape {
        FullImage,
        SquareCrop,
        VisibleImage,
        VisibleImageSquareCrop
    }

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

        val croppedBitmap = when (imageMode) {
            InputShape.FullImage -> bitmap
            InputShape.SquareCrop -> centerCropSquare(bitmap)
            InputShape.VisibleImage -> cropToAspectRatio(bitmap, canvasSize.value.width, canvasSize.value.height)
            InputShape.VisibleImageSquareCrop -> cropToAspectRatio(bitmap, canvasSize.value.width, canvasSize.value.height, true)
        }

        val letterboxResult = LetterboxBuilder.build(croppedBitmap, interpreter.inputImageWidth)
        val output: FloatArray = interpreter.runInference(letterboxResult.bitmap)

        return postProcessor.process(
            output = output,
            width = croppedBitmap.width,
            height = croppedBitmap.height,
            originalWidth = bitmap.width,
            originalHeight = bitmap.height,
            lbScale = letterboxResult.scale,
            padX = letterboxResult.padX,
            padY = letterboxResult.padY
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
        LetterboxBuilder.cleanUp()
    }
}