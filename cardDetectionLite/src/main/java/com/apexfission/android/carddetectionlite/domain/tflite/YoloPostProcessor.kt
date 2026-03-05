package com.apexfission.android.carddetectionlite.domain.tflite

import com.apexfission.android.carddetectionlite.domain.tflite.data.RawDet
import kotlin.math.max
import kotlin.math.min

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

    fun process(
        output: FloatArray,
        width: Int,
        height: Int,
        lbScale: Float,
        padX: Float,
        padY: Float
    ): List<RawDet> {
        val raw = decodeToCropNormalized(output, width, height, lbScale, padX, padY)
        val capped = if (raw.size > maxNmsCandidates) raw.sortedByDescending { it.confidence }.take(maxNmsCandidates) else raw
        return nms(capped)
    }

    private fun decodeToCropNormalized(
        output: FloatArray,
        cropW: Int,
        cropH: Int,
        lbScale: Float,
        padX: Float,
        padY: Float
    ): List<RawDet> {
        val rawDets = ArrayList<RawDet>(64)

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

            rawDets += RawDet(
                x1Pct = (x1C / cropW).coerceIn(0f, 1f),
                y1Pct = (y1C / cropH).coerceIn(0f, 1f),
                x2Pct = (x2C / cropW).coerceIn(0f, 1f),
                y2Pct = (y2C / cropH).coerceIn(0f, 1f),
                confidence = maxClassScore,
                classId = bestCls
            )
        }
        return rawDets
    }

    private fun nms(rawDets: List<RawDet>): List<RawDet> {
        val sorted = rawDets.sortedByDescending { it.confidence }.toMutableList()
        val keep = ArrayList<RawDet>()
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

    private fun iou(a: RawDet, b: RawDet): Float {
        val interW = max(0f, min(a.x2Pct, b.x2Pct) - max(a.x1Pct, b.x1Pct))
        val interH = max(0f, min(a.y2Pct, b.y2Pct) - max(a.y1Pct, b.y1Pct))
        val inter = interW * interH
        val areaA = (a.x2Pct - a.x1Pct) * (a.y2Pct - a.y1Pct)
        val areaB = (b.x2Pct - b.x1Pct) * (b.y2Pct - b.y1Pct)
        return inter / (areaA + areaB - inter + 1e-6f)
    }
}