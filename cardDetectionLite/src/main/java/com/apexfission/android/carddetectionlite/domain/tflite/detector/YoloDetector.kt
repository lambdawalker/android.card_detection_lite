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
import com.apexfission.android.carddetectionlite.domain.tflite.model.ExtractedFeatures
import com.apexfission.android.carddetectionlite.domain.tflite.model.buildDetection
import java.io.Closeable
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Interface for a detector that can detect objects in images.
 */
interface Detector : Closeable {
    /**
     * Whether the detector is enabled.
     */
    var enabled: Boolean

    /**
     * Detects objects in a bitmap.
     * @param bitmap The bitmap to detect objects in.
     * @return The detections.
     */
    fun detect(bitmap: Bitmap): Detections
    /**
     * Detects objects in an ImageProxy.
     * @param imageProxy The ImageProxy to detect objects in.
     * @return The detections.
     */
    fun detect(imageProxy: ImageProxy): Detections
    /**
     * Extracts features from an ImageProxy.
     * @param imageProxy The ImageProxy to extract features from.
     * @param maxCutouts The maximum number of cutouts to extract. 30 is a limit flexible enough to include cards, and smaller features (photos, qr, barcodes, etc.).
     * @return The extracted features.
     */
    fun extractFeatures(imageProxy: ImageProxy, maxCutouts: Int = 30): ExtractedFeatures
}

/**
 * A detector that uses a YOLO model to detect objects.
 * @param context The context.
 * @param modelPath The path to the model.
 * @param scoreThreshold The score threshold for detections.
 * @param iouThreshold The IOU threshold for non-max suppression.
 * @param useGpu Whether to use the GPU.
 * @param maxNmsCandidates The maximum number of candidates for non-max suppression.
 * @param numThreads The number of threads to use.
 * @param canvasSize The size of the canvas.
 * @param imageMode The image mode.
 */
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

    /**
     * Detects objects in a bitmap.
     * @param bitmap The bitmap to detect objects in.
     * @return The detections.
     */
    override fun detect(bitmap: Bitmap): Detections {
        if (!enabled || isClosed) return Detections(
            emptyList(), 0, 0, 0, 0
        )


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
            contextWidth = croppedBitmap.width,
            contextHeight = croppedBitmap.height,
            originalWidth = bitmap.width,
            originalHeight = bitmap.height,
            lbScale = letterboxResult.scale,
            padX = letterboxResult.padX,
            padY = letterboxResult.padY
        )

        return Detections(
            rawDetections,
            contextWidth = croppedBitmap.width,
            contextHeight = croppedBitmap.height,
            originalWidth = bitmap.width,
            originalHeight = bitmap.height,
        )
    }

    /**
     * Detects objects in an ImageProxy.
     * @param imageProxy The ImageProxy to detect objects in.
     * @return The detections.
     */
    override fun detect(imageProxy: ImageProxy): Detections {
        if (!enabled || isClosed) return Detections(
            emptyList(), 0, 0, 0, 0
        )
        val bitmap = imageProxy.toUprightBitmap()
        return detect(bitmap)
    }

    /**
     * Extracts features from an ImageProxy.
     * @param imageProxy The ImageProxy to extract features from.
     * @param maxCutouts The maximum number of cutouts to extract.
     * @return The extracted features.
     */
    override fun extractFeatures(imageProxy: ImageProxy, maxCutouts: Int): ExtractedFeatures {
        if (!enabled || isClosed) return ExtractedFeatures(emptyList(), 0, 0, 0, 0)

        val bitmap = imageProxy.toUprightBitmap()
        val rawDetections: Detections = detect(bitmap)

        val detections = rawDetections.detections.take(maxCutouts).map {
            buildDetection(bitmap, it, 0)
        }

        return ExtractedFeatures(
            detections,
            rawDetections.contextWidth,
            rawDetections.contextHeight,
            rawDetections.originalWidth,
            rawDetections.originalHeight
        )
    }

    /**
     * Closes the detector.
     */
    @Synchronized
    override fun close() {
        if (isClosed) return
        isClosed = true
        interpreter.close()
        LetterboxBuilder.cleanUp()
    }
}