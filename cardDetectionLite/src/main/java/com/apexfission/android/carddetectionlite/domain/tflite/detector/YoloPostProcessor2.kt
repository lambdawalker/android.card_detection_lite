package com.apexfission.android.carddetectionlite.domain.tflite.detector

import com.apexfission.android.carddetectionlite.domain.coordinates.ImageBox2P
import com.apexfission.android.carddetectionlite.domain.tflite.model.Detection
import com.apexfission.android.carddetectionlite.domain.tflite.model.Detection2
import com.apexfission.android.carddetectionlite.domain.tflite.model.LetterboxResult
import kotlin.math.max
import kotlin.math.min

/**
 * Handles the complex task of decoding and post-processing the raw output from a YOLO TFLite model.
 *
 * This class is responsible for converting the model's raw tensor (a flat `FloatArray`) into a
 * meaningful and clean list of [Detection] objects.
 *
 * ### Key Operations:
 * 1.  **Decoding**: Iterates through the raw output, identifying candidate bounding boxes and their
 *     most likely class based on confidence scores.
 * 2.  **Coordinate Transformation**: Maps the coordinates from the model's input space (e.g., a 640x640
 *     letterboxed image) back to the coordinate space of the original, un-padded, and un-cropped image.
 * 3.  **Non-Max Suppression (NMS)**: Eliminates redundant, overlapping bounding boxes for the same object,
 *     keeping only the one with the highest confidence score.
 *
 * @param outLayout The memory layout of the model's output tensor, as determined by [TfliteInterpreter].
 * @param outBoxes The total number of candidate bounding boxes the model produces.
 * @param outAttrs The number of attributes per box (e.g., 4 coordinates + N class scores).
 * @param numClasses The number of distinct object categories the model can detect.
 * @param inputImageWidth The width of the square image fed into the model (e.g., 640).
 * @param scoreThreshold The minimum confidence score (from 0.0 to 1.0) required for a detection
 *                       to be considered valid. Anything below this is discarded.
 * @param iouThreshold The Intersection-over-Union (IoU) threshold (from 0.0 to 1.0) for NMS. If two
 *                     boxes of the same class have an IoU greater than this value, the one with the
 *                     lower score is suppressed.
 * @param maxNmsCandidates The absolute maximum number of detections to return after NMS, even if more
 *                      are available.
 * @param outputScalingMode Defines how the raw bounding box coordinates from the model's tensor
 *                          are interpreted—either as normalized values (0..1) or as direct pixel values.
 */
class YoloPostProcessor2(
    private val outLayout: TfliteInterpreter.OutputLayout,
    private val outBoxes: Int,
    private val outAttrs: Int,
    private val numClasses: Int,
    private val inputImageWidth: Int,
    private val scoreThreshold: Float,
    private val iouThreshold: Float,
    private val maxNmsCandidates: Int,
    private val outputScalingMode: OutputScalingMode = OutputScalingMode.NORMALIZED
) {

    /** Defines how raw tensor coordinates are interpreted. */
    enum class OutputScalingMode {
        /** Assumes model outputs are raw pixel values (e.g., 0 to 640). No scaling is applied. */
        NONE,

        /** Assumes model outputs are normalized values (0.0 to 1.0) and scales them by [inputImageWidth]. */
        NORMALIZED
    }

    /**
     * Executes the entire post-processing pipeline on the raw output from the TFLite interpreter.
     *
     * @param output The flattened `FloatArray` directly from the TFLite model's output tensor.
     */
    fun process(
        output: FloatArray, letterboxResult: LetterboxResult
    ): List<Detection2> {
        val rawDetections = decodeDetections(
            output = output, lbScale = letterboxResult.scale, padX = letterboxResult.padX, padY = letterboxResult.padY
        )

        return nms(rawDetections)
    }

    /** Decodes the raw model output, reversing the letterboxing transformation. */
    private fun decodeDetections(
        output: FloatArray, lbScale: Float, padX: Float, padY: Float
    ): ArrayList<Detection2> {
        val detections = ArrayList<Detection2>(128)

        val isBoxesFirst = outLayout == TfliteInterpreter.OutputLayout.ATTRS_X_BOXES
        val strideBox = if (isBoxesFirst) 1 else outAttrs
        val strideAttr = if (isBoxesFirst) outBoxes else 1

        val modelInputImageWidth = if (outputScalingMode == OutputScalingMode.NONE) 1f else inputImageWidth.toFloat()

        for (i in 0 until outBoxes) {
            val bIdx = i * strideBox
            var maxClassScore = 0f
            var bestCls = -1

            // Find the class with the highest score for this box.
            for (c in 0 until numClasses) {
                val score = output[bIdx + (4 + c) * strideAttr]
                if (score > maxClassScore) {
                    maxClassScore = score
                    bestCls = c
                }
            }

            if (maxClassScore < scoreThreshold) continue

            // Extract coordinates.

            //centerPoint
            val cx = output[bIdx]
            val cy = output[bIdx + strideAttr]

            //size
            val w = output[bIdx + 2 * strideAttr]
            val h = output[bIdx + 3 * strideAttr]

            val halfW = (w * modelInputImageWidth) / 2f
            val halfH = (h * modelInputImageWidth) / 2f

            // Reverse the letterbox transformation: remove padding and apply inverse scale.
            val x1 = ((cx * modelInputImageWidth - halfW) - padX) / lbScale
            val y1 = ((cy * modelInputImageWidth - halfH) - padY) / lbScale
            val x2 = ((cx * modelInputImageWidth + halfW) - padX) / lbScale
            val y2 = ((cy * modelInputImageWidth + halfH) - padY) / lbScale

            // Normalize coordinates to the cropped image dimensions and store.
            detections += Detection2(
                ImageBox2P(
                    x = x1.toUInt(), y = y1.toUInt(), x2 = x2.toUInt(), y2 = y2.toUInt()
                ), maxClassScore, bestCls
            )
        }

        return detections
    }

    /** An optimized Non-Max Suppression algorithm. */
    private fun nms(detections: ArrayList<Detection2>): List<Detection2> {
        if (detections.isEmpty()) return emptyList()
        val sorted = detections.sortedByDescending { it.confidence }
        val size = sorted.size

        val areas = FloatArray(size) { i ->
            val box = sorted[i].box
            ((box.x2 - box.x) * (box.y2 - box.y)).toFloat()
        }

        val suppressed = BooleanArray(size)
        val keep = ArrayList<Detection2>(min(size, maxNmsCandidates))

        for (i in 0 until size) {
            if (suppressed[i]) continue
            val best = sorted[i]
            keep.add(best)
            if (keep.size >= maxNmsCandidates) break

            for (j in i + 1 until size) {
                if (suppressed[j]) continue
                val next = sorted[j]

                val isSameClass = next.classId == best.classId
                if (!isSameClass) continue

                val iou = calculateIoU(best.box, areas[i], next.box, areas[j])
                if (iou > iouThreshold) {
                    suppressed[j] = true
                }
            }
        }

        return keep
    }

    /** Calculates the Intersection over Union of two detections. */
    private fun calculateIoU(a: ImageBox2P, areaA: Float, b: ImageBox2P, areaB: Float): Float {
        if (a.x > b.x2 || a.x2 < b.x || a.y > b.y2 || a.y2 < b.y) return 0f

        val interW = max(0u, min(a.x2, b.x2) - max(a.x, b.x)).toFloat()
        val interH = max(0u, min(a.y2, b.y2) - max(a.y, b.y)).toFloat()
        val inter = interW * interH
        return (inter / (areaA + areaB - inter + 1e-6f))
    }
}
