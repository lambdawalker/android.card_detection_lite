package com.apexfission.android.carddetectionlite.domain.tflite.detector

import com.apexfission.android.carddetectionlite.domain.tflite.model.Detection
import kotlin.math.max
import kotlin.math.min

/**
 * Post-processor for YOLO models optimized for mobile real-time performance.
 *
 * @param outLayout The memory layout of the output tensor (Boxes-first or Attrs-first).
 * @param outBoxes Total number of candidate boxes produced by the model.
 * @param outAttrs Number of attributes per box (usually 4 coords + numClasses).
 * @param numClasses Number of object categories the model can detect.
 * @param inputImageWidth The width used for the model's input (e.g., 640).
 * @param scoreThreshold Minimum confidence to consider a detection valid.
 * @param iouThreshold Intersection-over-Union limit for suppressing duplicates.
 * @param maxNmsCandidates Maximum number of detections to return after NMS.
 * @param outputScalingMode Strategy for interpreting raw tensor coordinates. Defaults to [OutputScalingMode.NORMALIZED].
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

    /**
     * Defines how raw tensor coordinates are interpreted.
     */
    enum class OutputScalingMode {
        /** Assumes model outputs raw pixels (e.g., 0..640). Coordinates will be divided by 1.0. */
        NONE,
        /** Assumes model outputs normalized values (0..1). Coordinates will be multiplied by [inputImageWidth]. */
        NORMALIZED
    }

    /**
     * Processes raw TFLite output into a refined list of [Detection] objects.
     */
    fun process(
        output: FloatArray,
        contextWidth: Int,
        contextHeight: Int,
        lbScale: Float,
        padX: Float,
        padY: Float,
        originalWidth: Int,
        originalHeight: Int
    ): List<Detection> {
        val raw = decodeToCropNormalized(output, contextWidth, contextHeight, lbScale, padX, padY)

        val processed = if (contextWidth == originalWidth && contextHeight == originalHeight) {
            raw
        } else {
            mapFromCropToOriginal(raw, contextWidth, contextHeight, originalWidth, originalHeight)
        }

        return nms(processed)
    }

    /**
     * PERFORMANCE: Optimized "Hot Path"
     * Uses stride-based indexing and minimal allocations.
     */
    private fun decodeToCropNormalized(
        output: FloatArray, cropW: Int, cropH: Int, lbScale: Float, padX: Float, padY: Float
    ): List<Detection> {
        val detections = ArrayList<Detection>(128)
        val isBoxesFirst = outLayout == TfliteInterpreter.OutputLayout.ATTRS_X_BOXES
        val strideBox = if (isBoxesFirst) 1 else outAttrs
        val strideAttr = if (isBoxesFirst) outBoxes else 1

        val scale = if (outputScalingMode == OutputScalingMode.NONE) 1f else inputImageWidth.toFloat()

        for (i in 0 until outBoxes) {
            val bIdx = i * strideBox

            var maxClassScore = 0f
            var bestCls = -1

            // Hot Loop: Class identification
            for (c in 0 until numClasses) {
                val score = output[bIdx + (4 + c) * strideAttr]
                if (score > maxClassScore) {
                    maxClassScore = score
                    bestCls = c
                }
            }

            if (maxClassScore < scoreThreshold) continue

            val cxI = output[bIdx + 0 * strideAttr]
            val cyI = output[bIdx + 1 * strideAttr]
            val wI = output[bIdx + 2 * strideAttr]
            val hI = output[bIdx + 3 * strideAttr]

            val halfW = (wI * scale) / 2f
            val halfH = (hI * scale) / 2f

            // Map from Letterbox/Input space to Crop space
            val x1C = ((cxI * scale - halfW) - padX) / lbScale
            val y1C = ((cyI * scale - halfH) - padY) / lbScale
            val x2C = ((cxI * scale + halfW) - padX) / lbScale
            val y2C = ((cyI * scale + halfH) - padY) / lbScale

            detections += Detection(
                x1Pct = (x1C / cropW).coerceIn(0f, 1f),
                y1Pct = (y1C / cropH).coerceIn(0f, 1f),
                x2Pct = (x2C / cropW).coerceIn(0f, 1f),
                y2Pct = (y2C / cropH).coerceIn(0f, 1f),
                confidence = maxClassScore,
                classId = bestCls
            )
        }
        return detections
    }

    /**
     * MATH: Coordinate Transformation
     * Translates coordinates from a centered ROI back to full image space.
     */
    private fun mapFromCropToOriginal(
        detections: List<Detection>, width: Int, height: Int, originalWidth: Int, originalHeight: Int
    ): List<Detection> {
        val xFactor = width.toFloat() / originalWidth
        val yFactor = height.toFloat() / originalHeight
        val xOffset = (1f - xFactor) / 2f
        val yOffset = (1f - yFactor) / 2f

        return detections.map {
            it.copy(
                x1Pct = it.x1Pct * xFactor + xOffset,
                y1Pct = it.y1Pct * yFactor + yOffset,
                x2Pct = it.x2Pct * xFactor + xOffset,
                y2Pct = it.y2Pct * yFactor + yOffset
            )
        }
    }

    /**
     * ALGORITHM: Optimized NMS
     * Uses Boolean suppression and area caching to achieve better performance than O(N^2) list mutation.
     */
    private fun nms(detections: List<Detection>): List<Detection> {
        if (detections.isEmpty()) return emptyList()

        val sorted = detections.sortedByDescending { it.confidence }
        val size = sorted.size

        val areas = FloatArray(size) { i ->
            (sorted[i].x2Pct - sorted[i].x1Pct) * (sorted[i].y2Pct - sorted[i].y1Pct)
        }

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
                if (next.classId == best.classId) {
                    if (calculateIoU(best, areas[i], next, areas[j]) > iouThreshold) {
                        suppressed[j] = true
                    }
                }
            }
        }
        return keep
    }

    private fun calculateIoU(a: Detection, areaA: Float, b: Detection, areaB: Float): Float {
        // Fast-path: Check for separation
        if (a.x1Pct > b.x2Pct || a.x2Pct < b.x1Pct || a.y1Pct > b.y2Pct || a.y2Pct < b.y1Pct) {
            return 0f
        }

        val interW = max(0f, min(a.x2Pct, b.x2Pct) - max(a.x1Pct, b.x1Pct))
        val interH = max(0f, min(a.y2Pct, b.y2Pct) - max(a.y1Pct, b.y1Pct))
        val inter = interW * interH

        return inter / (areaA + areaB - inter + 1e-6f)
    }
}