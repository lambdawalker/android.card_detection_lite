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
 * Defines the contract for a generic object detector that is lifecycle-aware.
 */
interface Detector : Closeable {
    /**
     * Controls the active state of the detector. When `false`, detection calls should
     * return empty results immediately.
     */
    var enabled: Boolean

    /**
     * Performs object detection on a provided [Bitmap].
     * @param bitmap The input image for detection.
     * @return A [Detections] object containing the list of found objects and image metadata.
     */
    fun detect(bitmap: Bitmap): Detections

    /**
     * Performs object detection on a frame from the camera, encapsulated in an [ImageProxy].
     * @param imageProxy The camera frame to process.
     * @return A [Detections] object.
     */
    fun detect(imageProxy: ImageProxy): Detections

    /**
     * Detects objects and then extracts a cropped bitmap for each one.
     *
     * @param imageProxy The camera frame to process.
     * @param maxCutouts Limits the number of features to extract. This is useful for performance
     *                   and for focusing only on the most confident detections. A limit of 30 is
     *                   generally sufficient to capture a primary card and its smaller sub-features
     *                   (like photos, QR codes, etc.).
     * @return An [ExtractedFeatures] object containing a list of `ExtractedFeature`s,
     *         each with a cropped bitmap of the detected object.
     */
    fun extractFeatures(imageProxy: ImageProxy, maxCutouts: Int = 30): ExtractedFeatures
}

/**
 * An implementation of [Detector] that uses a YOLO (You Only Look Once) model with TensorFlow Lite.
 *
 * This class orchestrates the entire detection pipeline:
 * 1.  Image Preprocessing: Applies cropping and letterboxing based on the specified [InputShape].
 * 2.  Inference: Runs the TFLite model via [TfliteInterpreter].
 * 3.  Post-processing: Decodes the model's output, applies non-max suppression, and returns the results
 *     via [YoloPostProcessor].
 *
 * @param context The application context, used for loading the model from assets.
 * @param modelPath The path to the TFLite model file within the `assets` directory.
 * @param scoreThreshold The minimum confidence level for a detection to be considered.
 *                       Values range from 0.0 to 1.0. A higher value reduces false positives
 *                       but may miss less certain detections.
 * @param iouThreshold The threshold for Intersection over Union (IoU) used in Non-Max Suppression.
 *                     It determines how much overlap two bounding boxes can have before one is
 *                     discarded. A value of 0.45 is a common starting point.
 * @param useGpu If `true`, attempts to use the GPU delegate for faster inference.
 * @param maxNmsCandidates The maximum number of detections to consider before running Non-Max Suppression.
 *                         Reducing this can improve performance but may discard valid objects pre-emptively.
 * @param numThreads The number of threads to use for inference on the CPU. If null, a default is chosen.
 * @param canvasSize A `StateFlow` that provides the current dimensions of the UI canvas. This is crucial
 *                   for `VisibleImage` modes to calculate the correct aspect ratio for cropping.
 * @param imageMode Defines how the input image is preprocessed (e.g., cropped, letterboxed) before
 *                  being sent to the model.
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

    override fun detect(imageProxy: ImageProxy): Detections {
        if (!enabled || isClosed) return Detections(
            emptyList(), 0, 0, 0, 0
        )
        val bitmap = imageProxy.toUprightBitmap()
        return detect(bitmap)
    }

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
     * Releases the TFLite interpreter and other resources. This should be called
     * when the detector is no longer needed to prevent memory leaks.
     */
    @Synchronized
    override fun close() {
        if (isClosed) return
        isClosed = true
        interpreter.close()
        LetterboxBuilder.cleanUp()
    }
}
