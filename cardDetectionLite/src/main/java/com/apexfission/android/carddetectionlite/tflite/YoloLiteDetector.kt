package com.apexfission.android.carddetectionlite.tflite

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.os.SystemClock
import android.util.Log
import androidx.camera.core.ImageProxy
import com.apexfission.android.carddetectionlite.Det
import com.apexfission.android.carddetectionlite.util.DetCutout
import com.apexfission.android.carddetectionlite.util.YuvToRgbConverter
import com.apexfission.android.carddetectionlite.util.cropDet
import com.apexfission.android.carddetectionlite.util.intersectedWith
import com.apexfission.android.carddetectionlite.util.rotateRectToUpright

import java.io.Closeable
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import kotlin.math.max
import kotlin.math.min
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.CompatibilityList
import org.tensorflow.lite.gpu.GpuDelegate


class YoloLiteDetector(
    context: Context,
    modelName: String,
    private val scoreThreshold: Float,
    private val iouThreshold: Float,
    private val maxNmsCandidates: Int = 300,
    debugLogs: Boolean = false,
    numThreads: Int? = null,
) : Closeable {

    var enabled: Boolean = true

    private var interpreter: Interpreter
    private val yuvToRgb = YuvToRgbConverter()
    private var gpuDelegate: GpuDelegate? = null
    private var isClosed = false

    private val isInt8: Boolean
    private val inputImageWidth: Int

    private val inputBuffer: ByteBuffer
    private val pixelBuffer: IntArray

    private val outShape: IntArray
    private val outCount: Int
    private val outByteBuffer: ByteBuffer
    private val outFloats: FloatArray

    // Quantization params
    private val outScale: Float
    private val outZeroPoint: Int
    private val inScale: Float
    private val inZeroPoint: Int

    private enum class OutputLayout { ATTRS_X_BOXES, BOXES_X_ATTRS }

    private val outLayout: OutputLayout
    private val outAttrs: Int
    private val outBoxes: Int
    private val numClasses: Int

    // Letterbox reuse
    private var lbOut: Bitmap? = null
    private var lbCanvas: Canvas? = null
    private val lbPaint = Paint(Paint.FILTER_BITMAP_FLAG)
    private val black = Color.BLACK

    var lastInferenceTimeMs: Long = 0L
        private set

    init {
        val model = loadModelFile(context, modelName)

        // 1) Probe tensors
        val temp = Interpreter(model)
        val inputTensor = temp.getInputTensor(0)
        val outputTensor = temp.getOutputTensor(0)

        isInt8 = inputTensor.dataType() == DataType.INT8
        inputImageWidth = inputTensor.shape()[1]

        val oShape = outputTensor.shape()
        outShape = oShape
        outCount = oShape.reduce { a, b -> a * b }

        val dim1 = oShape[1]
        val dim2 = oShape[2]
        outLayout = if (dim2 >= 400 && dim1 <= 256) OutputLayout.ATTRS_X_BOXES else OutputLayout.BOXES_X_ATTRS

        outAttrs = if (outLayout == OutputLayout.ATTRS_X_BOXES) dim1 else dim2
        outBoxes = if (outLayout == OutputLayout.ATTRS_X_BOXES) dim2 else dim1
        numClasses = outAttrs - 4

        temp.close()

        pixelBuffer = IntArray(inputImageWidth * inputImageWidth)

        // 2) Real Setup
        val options = Interpreter.Options().apply {
            setNumThreads(numThreads ?: max(2, Runtime.getRuntime().availableProcessors() / 2))
        }

        if (!isInt8) {
            try {
                val compat = CompatibilityList()
                if (compat.isDelegateSupportedOnThisDevice) {
                    gpuDelegate = GpuDelegate(compat.bestOptionsForThisDevice)
                    options.addDelegate(gpuDelegate)
                    Log.i("YOLO", "GPU delegate enabled ✅ using compatibility list.")
                } else {
                    gpuDelegate = GpuDelegate()
                    options.addDelegate(gpuDelegate)
                    Log.i("YOLO", "GPU delegate enabled ✅ using a simple delegate")
                }
            } catch (t: Throwable) {
                Log.e("YOLO", "GPU delegate failed, using CPU. err=${t.message}")
                gpuDelegate = null
            }
        }

        interpreter = Interpreter(model, options)

        val inQ = interpreter.getInputTensor(0).quantizationParams()
        inScale = inQ.scale
        inZeroPoint = inQ.zeroPoint

        val outQ = interpreter.getOutputTensor(0).quantizationParams()
        outScale = outQ.scale
        outZeroPoint = outQ.zeroPoint

        val bytesPerChannel = if (isInt8) 1 else 4
        inputBuffer = ByteBuffer.allocateDirect(1 * inputImageWidth * inputImageWidth * 3 * bytesPerChannel).order(ByteOrder.nativeOrder())
        outByteBuffer = ByteBuffer.allocateDirect(outCount * bytesPerChannel).order(ByteOrder.nativeOrder())

        outFloats = FloatArray(outCount)

        Log.i("YOLO", "Initialized: Classes=$numClasses, Layout=$outLayout, Type=${if (isInt8) "INT8" else "FLOAT"}")
    }

    @Synchronized
    private fun runInference(bitmap: Bitmap) {
        if (isClosed || interpreter == null) return
        val startTime = SystemClock.uptimeMillis()

        if (isInt8) fillBitmapToByteBuffer(bitmap, inputBuffer)
        else fillBitmapToFloatBuffer(bitmap, inputBuffer)

        outByteBuffer.rewind()
        interpreter?.run(inputBuffer, outByteBuffer)

        outByteBuffer.rewind()
        if (isInt8) {
            for (i in 0 until outCount) {
                val q = outByteBuffer.get().toInt()
                outFloats[i] = (q - outZeroPoint) * outScale
            }
        } else {
            outByteBuffer.asFloatBuffer().get(outFloats)
        }

        lastInferenceTimeMs = SystemClock.uptimeMillis() - startTime
    }

    fun detect(bitmap: Bitmap): List<Det> {
        if (!enabled || isClosed) return emptyList()
        val lb = letterboxToSquareReusable(bitmap, inputImageWidth)
        runInference(lb.bitmap)

        val raw = decodeToCropNormalized(
            output = outFloats,
            scoreThresh = scoreThreshold,
            cropW = bitmap.width,
            cropH = bitmap.height,
            lbScale = lb.scale,
            padX = lb.padX,
            padY = lb.padY
        )

        val capped = if (raw.size > maxNmsCandidates) raw.sortedByDescending { it.score }.take(maxNmsCandidates) else raw
        return nms(capped, iouThreshold)
    }

    fun detect(imageProxy: ImageProxy): List<Det> {
        if (!enabled || isClosed) return emptyList()
        val (cropBitmap, lb) = prepareCropAndLetterbox(imageProxy)
        runInference(lb.bitmap)

        val raw = decodeToCropNormalized(
            output = outFloats,
            scoreThresh = scoreThreshold,
            cropW = cropBitmap.width,
            cropH = cropBitmap.height,
            lbScale = lb.scale,
            padX = lb.padX,
            padY = lb.padY
        )

        val capped = if (raw.size > maxNmsCandidates) raw.sortedByDescending { it.score }.take(maxNmsCandidates) else raw
        return nms(capped, iouThreshold)
    }

    fun detectCutouts(imageProxy: ImageProxy, padPx: Int = 8, maxCutouts: Int = 5): List<DetCutout> {
        if (!enabled) return emptyList()
        val (cropBitmap, lb) = prepareCropAndLetterbox(imageProxy)
        runInference(lb.bitmap)

        val raw = decodeToCropNormalized(
            output = outFloats,
            scoreThresh = scoreThreshold,
            cropW = cropBitmap.width,
            cropH = cropBitmap.height,
            lbScale = lb.scale,
            padX = lb.padX,
            padY = lb.padY
        )

        val dets = nms(raw, iouThreshold).sortedByDescending { it.score }.take(maxCutouts)
        return dets.map { cropDet(cropBitmap, it, padPx) }
    }

    private fun prepareCropAndLetterbox(imageProxy: ImageProxy): Pair<Bitmap, LetterboxResult> {
        val rotation = imageProxy.imageInfo.rotationDegrees
        val bitmap = yuvToRgb.toBitmap(imageProxy)
        val upright = rotateIfNeeded(bitmap, rotation)

        val uprightCropRect = rotateRectToUpright(
            imageProxy.cropRect, imageProxy.width, imageProxy.height, rotation
        ).intersectedWith(upright.width, upright.height)

        val cropBitmap = Bitmap.createBitmap(
            upright, uprightCropRect.left, uprightCropRect.top, uprightCropRect.width(), uprightCropRect.height()
        )

        val lb = letterboxToSquareReusable(cropBitmap, inputImageWidth)
        return cropBitmap to lb
    }

    private fun decodeToCropNormalized(
        output: FloatArray, scoreThresh: Float, cropW: Int, cropH: Int, lbScale: Float, padX: Float, padY: Float
    ): List<Det> {
        val dets = ArrayList<Det>(64)

        fun idx(attr: Int, box: Int): Int {
            return if (outLayout == OutputLayout.ATTRS_X_BOXES) (attr * outBoxes + box)
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

            if (maxClassScore < scoreThresh) continue

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

    private fun nms(dets: List<Det>, iouThresh: Float): List<Det> {
        val sorted = dets.sortedByDescending { it.score }.toMutableList()
        val keep = ArrayList<Det>()
        while (sorted.isNotEmpty()) {
            val best = sorted.removeAt(0)
            keep += best
            val it = sorted.iterator()

            while (it.hasNext()) {
                val d = it.next()
                if (d.cls == best.cls && iou(best, d) > iouThresh) it.remove()
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

    private fun fillBitmapToFloatBuffer(bm: Bitmap, buf: ByteBuffer) {
        buf.rewind()
        bm.getPixels(pixelBuffer, 0, inputImageWidth, 0, 0, inputImageWidth, inputImageWidth)
        for (v in pixelBuffer) {
            buf.putFloat(((v shr 16) and 0xFF) / 255f)
            buf.putFloat(((v shr 8) and 0xFF) / 255f)
            buf.putFloat((v and 0xFF) / 255f)
        }
    }

    private fun fillBitmapToByteBuffer(bm: Bitmap, buf: ByteBuffer) {
        buf.rewind()
        bm.getPixels(pixelBuffer, 0, inputImageWidth, 0, 0, inputImageWidth, inputImageWidth)
        val invScale = if (inScale != 0f) 1f / inScale else 1f
        for (v in pixelBuffer) {
            buf.put(quantizeToInt8(((v shr 16) and 0xFF) / 255f, invScale, inZeroPoint))
            buf.put(quantizeToInt8(((v shr 8) and 0xFF) / 255f, invScale, inZeroPoint))
            buf.put(quantizeToInt8((v and 0xFF) / 255f, invScale, inZeroPoint))
        }
    }

    private fun quantizeToInt8(v: Float, invScale: Float, zeroPoint: Int): Byte = (v * invScale + zeroPoint).toInt().coerceIn(-128, 127).toByte()

    private fun rotateIfNeeded(bm: Bitmap, deg: Int): Bitmap {
        if (deg == 0) return bm
        val m = Matrix().apply { postRotate(deg.toFloat()) }
        return Bitmap.createBitmap(bm, 0, 0, bm.width, bm.height, m, true)
    }

    private data class LetterboxResult(val bitmap: Bitmap, val scale: Float, val padX: Float, val padY: Float)

    private fun letterboxToSquareReusable(src: Bitmap, outSize: Int): LetterboxResult {
        val out = lbOut?.takeIf { it.width == outSize } ?: Bitmap.createBitmap(outSize, outSize, Bitmap.Config.ARGB_8888).also { lbOut = it }
        val canvas = lbCanvas ?: Canvas(out).also { lbCanvas = it }
        val scale = min(outSize / src.width.toFloat(), outSize / src.height.toFloat())
        val newW = (src.width * scale).toInt()
        val newH = (src.height * scale).toInt()
        val padX = (outSize - newW) / 2f
        val padY = (outSize - newH) / 2f
        canvas.drawColor(black)
        canvas.drawBitmap(src, null, RectF(padX, padY, padX + newW, padY + newH), lbPaint)
        return LetterboxResult(out, scale, padX, padY)
    }

    private fun loadModelFile(context: Context, assetPath: String): ByteBuffer {
        val fd = context.assets.openFd(assetPath)
        return FileInputStream(fd.fileDescriptor).channel.map(
            FileChannel.MapMode.READ_ONLY, fd.startOffset, fd.declaredLength
        ).order(ByteOrder.nativeOrder())
    }

    @Synchronized
    override fun close() {
        if (isClosed) return
        isClosed = true
        interpreter.close()
        gpuDelegate?.close()
        gpuDelegate = null
        lbOut = null
        lbCanvas = null
    }
}