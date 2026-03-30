package com.apexfission.android.carddetectionlite.domain.tflite.detector

import com.apexfission.android.carddetectionlite.domain.tflite.model.Detection
import kotlin.math.max
import kotlin.math.min

/**
 * Post-processor for YOLO models.
 *
 * @param outLayout The layout of the output tensor.
 * @param outBoxes The number of boxes in the output.
 * @param outAttrs The number of attributes in the output.
 * @param numClasses The number of classes.
 * @param inputImageWidth The width of the input image.
 * @param scoreThreshold The score threshold for detections.
 * @param iouThreshold The IOU threshold for non-max suppression.
 * @param maxNmsCandidates The maximum number of candidates for non-max suppression.
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
) {

    /**
     * Processes the output of a YOLO model.
     *
     * @param output The output of the model.
     * @param width The width of the image.
     * @param height The height of the image.
     * @param lbScale The letterbox scale.
     * @param padX The letterbox padding in the x direction.
     * @param padY The letterbox padding in the y direction.
     * @param originalWidth The original width of the image.
     * @param originalHeight The original height of the image.
     * @return A list of detections.
     */
    fun process(
        output: FloatArray, contextWidth: Int, contextHeight: Int, lbScale: Float, padX: Float, padY: Float, originalWidth: Int, originalHeight: Int
    ): List<Detection> {
        val raw: List<Detection> = decodeToCropNormalized(output, contextWidth, contextHeight, lbScale, padX, padY)
        val remapped = mapFromCropCoordinatesToTheOriginalImageSpace(raw, contextWidth, contextHeight, originalWidth, originalHeight)
        val capped = if (remapped.size > maxNmsCandidates) remapped.sortedByDescending { it.confidence }.take(maxNmsCandidates) else remapped
        return nms(capped)
    }

    /**
     * The input image could be a cropped version of the full image.
     * This function translate the coordinates of the detection to the original image size.
     */
    private fun mapFromCropCoordinatesToTheOriginalImageSpace(
        detections: List<Detection>, width: Int, height: Int, originalWidth: Int, originalHeight: Int
    ): List<Detection> {
        if (width == originalWidth && height == originalHeight) return detections

        val xMarginInPixels: Float = (originalWidth - width) / 2.toFloat()
        val xPercentageMargin: Float = xMarginInPixels / originalWidth.toFloat()
        val xTranslationFactor: Float = width / originalWidth.toFloat()

        val yMarginInPixels = (originalHeight - height) / 2.toFloat()
        val yPercentageMargin = yMarginInPixels / originalHeight.toFloat()
        val yTranslationFactor = height / originalHeight.toFloat()

        val remapped = detections.map {
            Detection(
                confidence = it.confidence,
                x1Pct = it.x1Pct * xTranslationFactor + xPercentageMargin,
                y1Pct = it.y1Pct * yTranslationFactor + yPercentageMargin,
                x2Pct = it.x2Pct * xTranslationFactor + xPercentageMargin,
                y2Pct = it.y2Pct * yTranslationFactor + yPercentageMargin,
                classId = it.classId
            )
        }
        return remapped
    }


    private fun decodeToCropNormalized(
        output: FloatArray, cropW: Int, cropH: Int, lbScale: Float, padX: Float, padY: Float
    ): List<Detection> {
        val detections = ArrayList<Detection>(64)

        fun idx(attr: Int, box: Int): Int {
            return if (outLayout == TfliteInterpreter.OutputLayout.ATTRS_X_BOXES) (attr * outBoxes + box)
            else (box * outAttrs + attr)
        }

        for (i in 0 until outBoxes) {
            // Find best class
            var maxClassScore = 0f
            var bestCls = -1

            for (c in 0 until numClasses) {
                val s = output[idx(4 + c, i)]
                if (s > maxClassScore) {
                    maxClassScore = s
                    bestCls = c
                }
            }

            if (maxClassScore < scoreThreshold) continue

            // Box coordinates
            val cxI = output[idx(0, i)]
            val cyI = output[idx(1, i)]
            val wI = output[idx(2, i)]
            val hI = output[idx(3, i)]

            // YOLOv11 TFLite usually outputs in pixel coordinates (0..640)
            // If the values are very small (0..1), we assume they are already normalized
            val needsScaling = maxOf(cxI, cyI, wI, hI) > 2f
            val scaleFactor = if (needsScaling) 1f else inputImageWidth.toFloat()

            val x1C = ((cxI * scaleFactor - (wI * scaleFactor) / 2f) - padX) / lbScale
            val y1C = ((cyI * scaleFactor - (hI * scaleFactor) / 2f) - padY) / lbScale
            val x2C = ((cxI * scaleFactor + (wI * scaleFactor) / 2f) - padX) / lbScale
            val y2C = ((cyI * scaleFactor + (hI * scaleFactor) / 2f) - padY) / lbScale

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

    private fun nms(detections: List<Detection>): List<Detection> {
        val sorted = detections.sortedByDescending { it.confidence }.toMutableList()
        val keep = ArrayList<Detection>()
        while (sorted.isNotEmpty()) {
            val best = sorted.removeAt(0)
            keep += best
            val it = sorted.iterator()

            while (it.hasNext()) {
                val d = it.next()
                if (d.classId == best.classId && iou(best, d) > iouThreshold) it.remove()
            }
        }
        return keep
    }

    private fun iou(a: Detection, b: Detection): Float {
        val interW = max(0f, min(a.x2Pct, b.x2Pct) - max(a.x1Pct, b.x1Pct))
        val interH = max(0f, min(a.y2Pct, b.y2Pct) - max(a.y1Pct, b.y1Pct))
        val inter = interW * interH
        val areaA = (a.x2Pct - a.x1Pct) * (a.y2Pct - a.y1Pct)
        val areaB = (b.x2Pct - b.x1Pct) * (b.y2Pct - b.y1Pct)
        return inter / (areaA + areaB - inter + 1e-6f)
    }
}