package com.apexfission.android.carddetectionlite.domain.tflite

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.camera.core.ImageProxy
import com.apexfission.android.carddetectionlite.domain.tflite.data.Det
import com.apexfission.android.carddetectionlite.domain.tflite.data.DetCutout
import com.apexfission.android.carddetectionlite.domain.tflite.data.LetterboxResult
import java.io.Closeable
import kotlin.math.max
import kotlin.math.min

class YoloLiteDetector(
    context: Context,
    modelPath: String,
    scoreThreshold: Float,
    iouThreshold: Float,
    useGpu: Boolean,
    private val detectionMargin: Int = 20,
    maxNmsCandidates: Int = 300,
    numThreads: Int? = null,
) : Closeable {

    var enabled: Boolean = true
    private val yuvToRgb = YuvToRgbConverter()
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

    val lastInferenceTimeMs: Long
        get() = interpreter.lastInferenceTimeMs

    fun detect(bitmap: Bitmap): List<Det> {
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

    fun detect(imageProxy: ImageProxy): List<Det> {
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
        ).sortedByDescending { it.score }
        Log.d("YOLO", ">>>>>>>>>>>>> ${dets.size}")
        val filteredDets = dets.filter {
            it.x1 >= detectionMargin &&
            it.y1 >= detectionMargin &&
            it.x2 <= cropBitmap.width - detectionMargin &&
            it.y2 <= cropBitmap.height - detectionMargin
        }.take(maxCutouts)
        Log.d("YOLO", ">>>>>>>>>>>>> ${filteredDets.size}")
        return filteredDets.map {
            val detectionMargin = detectionMargin / 2
            val paddedDet = it.copy(
                x1 = max(0f, it.x1 - detectionMargin),
                y1 = max(0f, it.y1 - detectionMargin),
                x2 = min(cropBitmap.width.toFloat(), it.x2 + detectionMargin),
                y2 = min(cropBitmap.height.toFloat(), it.y2 + detectionMargin)
            )
            cropDet(cropBitmap, paddedDet, 0)
        }
    }

    private fun prepareCropAndLetterbox(imageProxy: ImageProxy): Pair<Bitmap, LetterboxResult> {
        val rotation = imageProxy.imageInfo.rotationDegrees
        val bitmap = yuvToRgb.toBitmap(imageProxy)
        val upright = ImageProcessor.rotateIfNeeded(bitmap, rotation)

        val uprightCropRect = rotateRectToUpright(
            imageProxy.cropRect, imageProxy.width, imageProxy.height, rotation
        ).intersectedWith(upright.width, upright.height)

        val cropBitmap = Bitmap.createBitmap(
            upright, uprightCropRect.left, uprightCropRect.top, uprightCropRect.width(), uprightCropRect.height()
        )

        val lb = ImageProcessor.letterboxToSquareReusable(cropBitmap, interpreter.inputImageWidth)
        return cropBitmap to lb
    }



    @Synchronized
    override fun close() {
        if (isClosed) return
        isClosed = true
        interpreter.close()
        ImageProcessor.cleanUp()
    }
}