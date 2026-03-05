package com.apexfission.android.carddetectionlite.domain.tflite

import android.content.Context
import android.graphics.Bitmap
import androidx.camera.core.ImageProxy

import com.apexfission.android.carddetectionlite.domain.tflite.data.RawDet
import com.apexfission.android.carddetectionlite.domain.tflite.data.DetCutout
import com.apexfission.android.carddetectionlite.domain.tflite.data.LetterboxResult
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
            cropW = bitmap.width,
            cropH = bitmap.height,
            lbScale = lb.scale,
            padX = lb.padX,
            padY = lb.padY
        )
    }

    fun detect(imageProxy: ImageProxy): List<RawDet> {
        if (!enabled || isClosed) return emptyList()

        val (cropBitmap, lb) = prepareCropAndLetterbox(imageProxy)
        val output = interpreter.runInference(lb.bitmap)

        return postProcessor.process(
            output = output,
            cropW = cropBitmap.width,
            cropH = cropBitmap.height,
            lbScale = lb.scale,
            padX = lb.padX,
            padY = lb.padY
        )
    }

    fun detectCutouts(imageProxy: ImageProxy, maxCutouts: Int = 5): List<DetCutout> {
        if (!enabled) return emptyList()

        val (cropBitmap, lb) = prepareCropAndLetterbox(imageProxy)
        val output = interpreter.runInference(lb.bitmap)

        val dets = postProcessor.process(
            output = output,
            cropW = cropBitmap.width,
            cropH = cropBitmap.height,
            lbScale = lb.scale,
            padX = lb.padX,
            padY = lb.padY
        ).sortedByDescending { it.confidence }

        val filteredDets = detectionFilters.fold(dets) { filtered, filter ->
            filter.filter(filtered, cropBitmap.width, cropBitmap.height)
        }.take(maxCutouts)

        return filteredDets.map {
            cropDet(cropBitmap, it, 0)
        }
    }



    private fun prepareCropAndLetterbox(imageProxy: ImageProxy): Pair<Bitmap, LetterboxResult> {
        val rotation = imageProxy.imageInfo.rotationDegrees
        val bitmap = imageProxy.toBitmap()
        val upright = ImageProcessor.rotateIfNeeded(bitmap, rotation)

        val lb = ImageProcessor.letterboxToSquareReusable(upright, interpreter.inputImageWidth)
        return upright to lb
    }


    @Synchronized
    override fun close() {
        if (isClosed) return
        isClosed = true
        interpreter.close()
        ImageProcessor.cleanUp()
    }
}