package com.apexfission.android.carddetectionlite.domain.tflite.detector.yolo.engine

import android.content.Context
import android.graphics.Bitmap
import androidx.camera.core.ImageProxy
import com.apexfission.android.carddetectionlite.domain.tflite.detector.tflite.engine.InferenceEngine
import com.apexfission.android.carddetectionlite.domain.tflite.detector.tflite.engine.buildThreadConfinedInferenceEngine
import com.apexfission.android.carddetectionlite.domain.tflite.detector.yolo.postprocess.YoloPostProcessor
import com.apexfission.android.carddetectionlite.domain.tflite.image.LetterboxBuilder
import com.apexfission.android.carddetectionlite.domain.tflite.image.toUprightBitmap
import com.apexfission.android.carddetectionlite.domain.tflite.model.Detection
import com.apexfission.android.carddetectionlite.domain.tflite.model.LetterboxResult
import com.apexfission.android.carddetectionlite.ui.detector.NumThreads
import java.util.concurrent.atomic.AtomicBoolean

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

    private val interpreter: InferenceEngine = buildThreadConfinedInferenceEngine(context, modelPath, useGpu, numThreads)
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
