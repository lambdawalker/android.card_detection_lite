package com.apexfission.android.carddetectionlite.domain.tflite.detector

import android.content.Context
import android.graphics.Bitmap
import androidx.camera.core.ImageProxy
import com.apexfission.android.carddetectionlite.domain.tflite.image.LetterboxBuilder
import com.apexfission.android.carddetectionlite.domain.tflite.image.toUprightBitmap
import com.apexfission.android.carddetectionlite.domain.tflite.model.Detection
import com.apexfission.android.carddetectionlite.domain.tflite.model.LetterboxResult
import com.apexfission.android.carddetectionlite.ui.detector.NumThreads
import java.io.Closeable
import java.util.concurrent.atomic.AtomicBoolean


/**
 * Defines the contract for a generic object detector that is lifecycle-aware.
 */
interface Detector : Closeable {
    /**
     * Controls the active state of the detector. When `false`, detection calls should
     * return empty results immediately.
     */
    var enabled: Boolean


    fun detect(bitmap: Bitmap): List<Detection>


    fun detect(imageProxy: ImageProxy): List<Detection>
}


class YoloDetector(
    context: Context,
    modelPath: String,
    scoreThreshold: Float,
    iouThreshold: Float,
    useGpu: Boolean,
    maxNmsCandidates: Int = 150,
    numThreads: NumThreads = NumThreads.Default,
) : Detector {
    private val _enabled = AtomicBoolean(true)
    override var enabled: Boolean
        get() = _enabled.get()
        set(value) {
            _enabled.set(value)
        }

    private val isClosed = AtomicBoolean(false)

    private val interpreter = TfliteInterpreter(context, modelPath, useGpu, numThreads)
    private val letterboxBuilder = LetterboxBuilder()

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

    @Synchronized
    override fun detect(bitmap: Bitmap): List<Detection> {
        if (!enabled || isClosed.get()) return emptyList()

        val letterboxResult: LetterboxResult = letterboxBuilder.build(bitmap, interpreter.inputImageWidth)
        val output: FloatArray = interpreter.runInference(letterboxResult.bitmap)

        return postProcessor.process(
            output = output,
            letterboxResult = letterboxResult
        )
    }

    override fun detect(imageProxy: ImageProxy): List<Detection> {
        if (!enabled || isClosed.get()) return emptyList()
        val bitmap = imageProxy.toUprightBitmap()
        return try {
            detect(bitmap)
        } finally {
            bitmap.recycle()
        }
    }


    @Synchronized
    override fun close() {
        if (isClosed.getAndSet(true)) return
        runCatching {
            interpreter.close()
        }

        runCatching {
            letterboxBuilder.close()
        }
    }
}

