package com.apexfission.android.carddetectionlite.domain.tflite.detector

import com.apexfission.android.carddetectionlite.domain.tflite.model.Detection
import com.apexfission.android.carddetectionlite.domain.tflite.model.RawDetection
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
class YoloPostProcessor(
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
     * @param contextWidth The width of the image after any initial cropping, but before letterboxing.
     *                     This is the coordinate space the decoded detections will be relative to first.
     * @param contextHeight The height of the image after any initial cropping.
     * @param lbScale The scale factor that was applied to the cropped image to fit it into the
     *                letterbox canvas.
     * @param padX The horizontal padding (on one side) added during letterboxing.
     * @param padY The vertical padding (on one side) added during letterboxing.
     * @param originalWidth The width of the original, full-sized camera image.
     * @param originalHeight The height of the original, full-sized camera image.
     * @return A final, clean list of [Detection] objects with coordinates normalized to the `originalWidth`
     *         and `originalHeight`.
     */
    fun process(
        output: FloatArray, contextWidth: Int, contextHeight: Int, lbScale: Float, padX: Float, padY: Float, originalWidth: Int, originalHeight: Int
    ): List<Detection> {
        // Step 1: Decode the flat float array into detections, removing the letterbox padding.
        // The coordinates are now relative to the 'context' (cropped) image.
        val raw = decodeToCropNormalized(output, contextWidth, contextHeight, lbScale, padX, padY)

        // Step 2: Remap coordinates from the 'context' space to the 'original' full image space.
        val processed = if (contextWidth == originalWidth && contextHeight == originalHeight) {
            // If there was no initial crop, the context and original are the same.
            raw.map {
                Detection(it.x1Pct, it.y1Pct, it.x2Pct, it.y2Pct, it.x1Pct, it.y1Pct, it.x2Pct, it.y2Pct, it.confidence, it.classId)
            }
        } else {
            mapFromCropToOriginal(raw, contextWidth, contextHeight, originalWidth, originalHeight)
        }

        // Step 3: Apply Non-Max Suppression to eliminate duplicate detections.
        return nms(processed)
    }

    /** Decodes the raw model output, reversing the letterboxing transformation. */
    private fun decodeToCropNormalized(
        output: FloatArray, cropW: Int, cropH: Int, lbScale: Float, padX: Float, padY: Float
    ): List<RawDetection> {
        val detections = ArrayList<RawDetection>(128)
        val isBoxesFirst = outLayout == TfliteInterpreter.OutputLayout.ATTRS_X_BOXES
        val strideBox = if (isBoxesFirst) 1 else outAttrs
        val strideAttr = if (isBoxesFirst) outBoxes else 1
        val scale = if (outputScalingMode == OutputScalingMode.NONE) 1f else inputImageWidth.toFloat()

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
            val cxI = output[bIdx]
            val cyI = output[bIdx + strideAttr]
            val wI = output[bIdx + 2 * strideAttr]
            val hI = output[bIdx + 3 * strideAttr]
            val halfW = (wI * scale) / 2f
            val halfH = (hI * scale) / 2f

            // Reverse the letterbox transformation: remove padding and apply inverse scale.
            val x1C = ((cxI * scale - halfW) - padX) / lbScale
            val y1C = ((cyI * scale - halfH) - padY) / lbScale
            val x2C = ((cxI * scale + halfW) - padX) / lbScale
            val y2C = ((cyI * scale + halfH) - padY) / lbScale

            // Normalize coordinates to the cropped image dimensions and store.
            detections += RawDetection(
                (x1C / cropW).coerceIn(0f, 1f),
                (y1C / cropH).coerceIn(0f, 1f),
                (x2C / cropW).coerceIn(0f, 1f),
                (y2C / cropH).coerceIn(0f, 1f),
                maxClassScore,
                bestCls
            )
        }
        return detections
    }

    /** Translates coordinates from a centered crop/ROI back to the full original image space. */
    private fun mapFromCropToOriginal(
        detections: List<RawDetection>, width: Int, height: Int, originalWidth: Int, originalHeight: Int
    ): List<Detection> {
        val xFactor = width.toFloat() / originalWidth
        val yFactor = height.toFloat() / originalHeight
        val xOffset = (1f - xFactor) / 2f
        val yOffset = (1f - yFactor) / 2f
        return detections.map {
            Detection(
                it.x1Pct * xFactor + xOffset,
                it.y1Pct * yFactor + yOffset,
                it.x2Pct * xFactor + xOffset,
                it.y2Pct * yFactor + yOffset,
                it.x1Pct,
                it.y1Pct,
                it.x2Pct,
                it.y2Pct,
                it.confidence,
                it.classId
            )
        }
    }

    /** An optimized Non-Max Suppression algorithm. */
    private fun nms(detections: List<Detection>): List<Detection> {
        if (detections.isEmpty()) return emptyList()
        val sorted = detections.sortedByDescending { it.confidence }
        val size = sorted.size
        val areas = FloatArray(size) { i -> (sorted[i].x2Pct - sorted[i].x1Pct) * (sorted[i].y2Pct - sorted[i].y1Pct) }
        val suppressed = BooleanArray(size)
        val keep = ArrayList<Detection>(min(size, maxNmsCandidates))

        for (i in 0 until size) {
            if (suppressed[i]) continue
            val best = sorted[i]
            keep.add(best)
            if (keep.size >= maxNmsCandidates) break

            for (j in i + 1 until size) {
                if (suppressed[j]) continue
                val next = sorted[j]
                if (next.classId == best.classId && calculateIoU(best, areas[i], next, areas[j]) > iouThreshold) {
                    suppressed[j] = true
                }
            }
        }
        return keep
    }

    /** Calculates the Intersection over Union of two detections. */
    private fun calculateIoU(a: Detection, areaA: Float, b: Detection, areaB: Float): Float {
        if (a.x1Pct > b.x2Pct || a.x2Pct < b.x1Pct || a.y1Pct > b.y2Pct || a.y2Pct < b.y1Pct) return 0f
        val interW = max(0f, min(a.x2Pct, b.x2Pct) - max(a.x1Pct, b.x1Pct))
        val interH = max(0f, min(a.y2Pct, b.y2Pct) - max(a.y1Pct, b.y1Pct))
        val inter = interW * interH
        return inter / (areaA + areaB - inter + 1e-6f)
    }
}
