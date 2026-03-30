package com.apexfission.android.carddetectionlite.domain.tflite.detector

import android.content.Context
import android.graphics.Bitmap
import android.os.SystemClock
import android.util.Log
import java.io.Closeable
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import kotlin.math.max
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.CompatibilityList
import org.tensorflow.lite.gpu.GpuDelegate


/**
 * TODO: Refactor and Optimization Roadmap for TfliteInterpreter
 * * ## 1. Performance & Image Processing [Priority: High]
 * - [ ] **Optimize Manual Pixel Loop**: Use tensorflow libraries instead of local
 * functions. The problem comes with tensorflow libraries dependencies management
 *
 * ## 2. Metadata & Configuration [Priority: Medium]
 * - [ ] **Decouple Metadata**: Instead of probing the model with a temporary
 * Interpreter (which doubles memory during init), pass a Metadata object
 * containing [outLayout], [outAttrs], and [outBoxes] alongside the [modelPath].
 */

/**
 * A wrapper for the TFLite interpreter.
 *
 * @param context The context.
 * @param modelPath The path to the model.
 * @param useGpu Whether to use the GPU.
 * @param numThreads The number of threads to use in case of not using the GPU.
 */
class TfliteInterpreter(
    context: Context,
    modelPath: String,
    useGpu: Boolean,
    numThreads: Int?
) : Closeable {
    companion object {
        val tag = "TfliteInterpreter"
    }

    private var interpreter: Interpreter
    private var gpuDelegate: GpuDelegate? = null
    private var isClosed = false

    private val inputBuffer: ByteBuffer
    private val pixelBuffer: IntArray

    private val outCount: Int
    private val outByteBuffer: ByteBuffer
    private val outFloats: FloatArray

    private val inScale: Float
    private val inZeroPoint: Int
    private val outScale: Float
    private val outZeroPoint: Int

    /**
     * Whether the model is quantized to INT8.
     */
    val isInt8: Boolean

    /**
     * The width of the input image.
     */
    val inputImageWidth: Int


    /**
     * The layout of the output tensor.
     */
    enum class OutputLayout { ATTRS_X_BOXES, BOXES_X_ATTRS }

    /**
     * The layout of the output tensor.
     */
    val outLayout: OutputLayout

    /**
     * The number of attributes per detection.
     */
    val outAttrs: Int

    /**
     * The number of boxes per detection.
     */
    val outBoxes: Int

    /**
     * The number of classes.
     */
    val numClasses: Int

    /**
     * The time it took to run the last inference in milliseconds.
     */
    var lastInferenceTimeMs: Long = 0L
        private set


    init {
        val modelBuffer = loadModelFile(context, modelPath)

        val temp = Interpreter(modelBuffer)
        val inputTensor = temp.getInputTensor(0)
        val outputTensor = temp.getOutputTensor(0)

        isInt8 = inputTensor.dataType() == DataType.INT8
        inputImageWidth = inputTensor.shape()[1]

        val oShape = outputTensor.shape()
        outCount = oShape.reduce { a, b -> a * b }

        val dim1 = oShape[1]
        val dim2 = oShape[2]
        outLayout = if (dim2 >= 400 && dim1 <= 256) OutputLayout.ATTRS_X_BOXES else OutputLayout.BOXES_X_ATTRS

        outAttrs = if (outLayout == OutputLayout.ATTRS_X_BOXES) dim1 else dim2
        outBoxes = if (outLayout == OutputLayout.ATTRS_X_BOXES) dim2 else dim1
        numClasses = outAttrs - 4

        temp.close()

        Log.i(tag, "Classes $numClasses")
        Log.i(tag, "Boxes $outBoxes")
        Log.i(tag, "Attrs $outAttrs")
        Log.i(tag, "Layout $outLayout")
        Log.i(tag, "Int8 $isInt8")
        Log.i(tag, "Input width $inputImageWidth")


        pixelBuffer = IntArray(inputImageWidth * inputImageWidth)

        val options = Interpreter.Options().apply {
            setNumThreads(numThreads ?: max(2, Runtime.getRuntime().availableProcessors() / 2))
        }

        if (useGpu && !isInt8) {
            try {
                val compat = CompatibilityList()
                if (compat.isDelegateSupportedOnThisDevice) {
                    gpuDelegate = GpuDelegate(compat.bestOptionsForThisDevice)
                    options.addDelegate(gpuDelegate)
                } else {
                    gpuDelegate = GpuDelegate()
                    options.addDelegate(gpuDelegate)
                }
            } catch (_: Throwable) {
                gpuDelegate = null
            }
        }

        interpreter = Interpreter(modelBuffer, options)

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
    }

    /**
     * Runs inference on a bitmap.
     *
     * @param bitmap The bitmap to run inference on.
     * @return The output of the model.
     */
    @Synchronized
    fun runInference(bitmap: Bitmap): FloatArray {
        if (isClosed) return FloatArray(0)
        val startTime = SystemClock.uptimeMillis()

        if (isInt8) fillBitmapToByteBuffer(bitmap, inputBuffer)
        else fillBitmapToFloatBuffer(bitmap, inputBuffer)

        outByteBuffer.rewind()
        interpreter.run(inputBuffer, outByteBuffer)

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
        return outFloats
    }

    private fun fillBitmapToFloatBuffer(bm: Bitmap, buf: ByteBuffer) {
        buf.rewind()
        bm.getPixels(pixelBuffer, 0, inputImageWidth, 0, 0, inputImageWidth, inputImageWidth)

        val inv255 = 1.0f / 255.0f
        for (v in pixelBuffer) {
            buf.putFloat(((v shr 16) and 0xFF) * inv255)
            buf.putFloat(((v shr 8) and 0xFF) * inv255)
            buf.putFloat((v and 0xFF) * inv255)
        }
    }

    private fun fillBitmapToByteBuffer(bm: Bitmap, buf: ByteBuffer) {
        buf.rewind()
        bm.getPixels(pixelBuffer, 0, inputImageWidth, 0, 0, inputImageWidth, inputImageWidth)

        // Combine normalization (1/255) and quantization scale (1/inScale)
        // into a single constant to multiply by.
        val invScale = if (inScale != 0f) 1f / inScale else 1f
        val combinedMultiplier = (1.0f / 255.0f) * invScale

        for (v in pixelBuffer) {
            // Red channel
            buf.put(quantizeToInt8(((v shr 16) and 0xFF) * combinedMultiplier, inZeroPoint))
            // Green channel
            buf.put(quantizeToInt8(((v shr 8) and 0xFF) * combinedMultiplier, inZeroPoint))
            // Blue channel
            buf.put(quantizeToInt8((v and 0xFF) * combinedMultiplier, inZeroPoint))
        }
    }

    /**
     * Updated helper to accept the pre-multiplied value.
     */
    private fun quantizeToInt8(v: Float, zeroPoint: Int): Byte = (v + zeroPoint).toInt().coerceIn(-128, 127).toByte()

    private fun loadModelFile(context: Context, assetPath: String): ByteBuffer {
        val fd = context.assets.openFd(assetPath)
        return FileInputStream(fd.fileDescriptor).channel.map(
            FileChannel.MapMode.READ_ONLY, fd.startOffset, fd.declaredLength
        ).order(ByteOrder.nativeOrder())
    }

    /**
     * Closes the interpreter.
     */
    @Synchronized
    override fun close() {
        if (isClosed) return
        isClosed = true
        interpreter.close()
        gpuDelegate?.close()
        gpuDelegate = null
    }
}