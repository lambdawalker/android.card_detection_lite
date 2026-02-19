package com.apexfission.android.carddetectionlite.domain.tflite

import android.content.Context
import android.graphics.Bitmap
import androidx.camera.core.ImageProxy
import com.apexfission.android.carddetectionlite.domain.tflite.data.Det
import com.apexfission.android.carddetectionlite.domain.tflite.data.DetCutout
import com.apexfission.android.carddetectionlite.domain.tflite.data.LetterboxResult
import java.io.Closeable

class YoloLiteDetector(
    context: Context,
    modelName: String,
    scoreThreshold: Float,
    iouThreshold: Float,
    maxNmsCandidates: Int = 300,
    debugLogs: Boolean = false,
    numThreads: Int? = null,
) : Closeable {

    var enabled: Boolean = true
    private val yuvToRgb = YuvToRgbConverter()
    private var isClosed = false

    private val interpreter = TfliteInterpreter(context, modelName, numThreads)
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

    fun detectCutouts(imageProxy: ImageProxy, padPx: Int = 8, maxCutouts: Int = 5): List<DetCutout> {
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
        ).sortedByDescending { it.score }.take(maxCutouts)

        return dets.map { cropDet(cropBitmap, it, padPx) }
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