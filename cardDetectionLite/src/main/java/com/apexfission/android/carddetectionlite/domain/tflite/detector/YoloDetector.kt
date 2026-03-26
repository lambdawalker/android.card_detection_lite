package com.apexfission.android.carddetectionlite.domain.tflite.detector

import android.content.Context
import android.graphics.Bitmap
import androidx.camera.core.ImageProxy
import androidx.compose.ui.unit.IntSize
import com.apexfission.android.carddetectionlite.domain.tflite.image.LetterboxBuilder
import com.apexfission.android.carddetectionlite.domain.tflite.image.centerCropSquare
import com.apexfission.android.carddetectionlite.domain.tflite.image.cropToAspectRatio
import com.apexfission.android.carddetectionlite.domain.tflite.image.toUprightBitmap
import com.apexfission.android.carddetectionlite.domain.tflite.model.Detections
import com.apexfission.android.carddetectionlite.domain.tflite.model.RawDetections
import com.apexfission.android.carddetectionlite.domain.tflite.model.buildDetection
import java.io.Closeable
import kotlinx.coroutines.flow.MutableStateFlow

interface Detector : Closeable {
    var enabled: Boolean

    fun detect(bitmap: Bitmap): RawDetections
    fun detect(imageProxy: ImageProxy): RawDetections
    fun detectCutouts(imageProxy: ImageProxy, maxCutouts: Int = 5): Detections
}

class YoloDetector(
    context: Context,
    modelPath: String,
    scoreThreshold: Float,
    iouThreshold: Float,
    useGpu: Boolean,
    maxNmsCandidates: Int = 300,
    numThreads: Int? = null,
    private val canvasSize: MutableStateFlow<IntSize>,
    private val imageMode: InputShape,
) : Detector {
    override var enabled: Boolean = true
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

    override fun detect(bitmap: Bitmap): RawDetections {
        if (!enabled || isClosed) return RawDetections(emptyList(), bitmap.width, bitmap.height)


        val croppedBitmap = when (imageMode) {
            InputShape.FullImage -> bitmap
            InputShape.SquareCrop -> centerCropSquare(bitmap)
            InputShape.VisibleImage -> cropToAspectRatio(bitmap, canvasSize.value.width, canvasSize.value.height)
            InputShape.VisibleImageSquareCrop -> cropToAspectRatio(bitmap, canvasSize.value.width, canvasSize.value.height, true)
        }

        val letterboxResult = LetterboxBuilder.build(croppedBitmap, interpreter.inputImageWidth)
        val output: FloatArray = interpreter.runInference(letterboxResult.bitmap)

        val rawDetections = postProcessor.process(
            output = output,
            width = croppedBitmap.width,
            height = croppedBitmap.height,
            originalWidth = bitmap.width,
            originalHeight = bitmap.height,
            lbScale = letterboxResult.scale,
            padX = letterboxResult.padX,
            padY = letterboxResult.padY
        )

        return RawDetections(
            rawDetections,
            croppedBitmap.width,
            croppedBitmap.height
        )
    }

    override fun detect(imageProxy: ImageProxy): RawDetections {
        if (!enabled || isClosed) return RawDetections(emptyList(), imageProxy.width, imageProxy.height)
        val bitmap = imageProxy.toUprightBitmap()
        return detect(bitmap)
    }

    override fun detectCutouts(imageProxy: ImageProxy, maxCutouts: Int): Detections {
        if (!enabled || isClosed) return Detections(emptyList(), imageProxy.width, imageProxy.height)

        val bitmap = imageProxy.toUprightBitmap()
        val rawDetections = detect(bitmap)

        val detections = rawDetections.rawDetections.take(maxCutouts).map {
            buildDetection(bitmap, it, 0)
        }

        return Detections(detections, rawDetections.imageWidth, rawDetections.imageHeight)
    }

    @Synchronized
    override fun close() {
        if (isClosed) return
        isClosed = true
        interpreter.close()
        LetterboxBuilder.cleanUp()
    }
}