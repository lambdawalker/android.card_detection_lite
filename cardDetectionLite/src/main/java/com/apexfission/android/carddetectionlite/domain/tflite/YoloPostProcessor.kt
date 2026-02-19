package com.apexfission.android.carddetectionlite.domain.tflite

import com.apexfission.android.carddetectionlite.domain.tflite.data.Det
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
        cropW: Int,
        cropH: Int,
        lbScale: Float,
        padX: Float,
        padY: Float
    ): List<Det> {
        val raw = decodeToCropNormalized(output, cropW, cropH, lbScale, padX, padY)
        val capped = if (raw.size > maxNmsCandidates) raw.sortedByDescending { it.score }.take(maxNmsCandidates) else raw
        return nms(capped)
    }

    private fun decodeToCropNormalized(
        output: FloatArray,
        cropW: Int,
        cropH: Int,
        lbScale: Float,
        padX: Float,
        padY: Float
    ): List<Det> {
        val dets = ArrayList<Det>(64)

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

            dets += Det(
                (x1C / cropW).coerceIn(0f, 1f),
                (y1C / cropH).coerceIn(0f, 1f),
                (x2C / cropW).coerceIn(0f, 1f),
                (y2C / cropH).coerceIn(0f, 1f),
                maxClassScore,
                bestCls
            )
        }
        return dets
    }

    private fun nms(dets: List<Det>): List<Det> {
        val sorted = dets.sortedByDescending { it.score }.toMutableList()
        val keep = ArrayList<Det>()
        while (sorted.isNotEmpty()) {
            val best = sorted.removeAt(0)
            keep += best
            val it = sorted.iterator()

            while (it.hasNext()) {
                val d = it.next()
                if (d.cls == best.cls && iou(best, d) > iouThreshold) it.remove()
            }
        }
        return keep
    }

    private fun iou(a: Det, b: Det): Float {
        val interW = max(0f, min(a.x2, b.x2) - max(a.x1, b.x1))
        val interH = max(0f, min(a.y2, b.y2) - max(a.y1, b.y1))
        val inter = interW * interH
        val areaA = (a.x2 - a.x1) * (a.y2 - a.y1)
        val areaB = (b.x2 - b.x1) * (b.y2 - b.y1)
        return inter / (areaA + areaB - inter + 1e-6f)
    }
}