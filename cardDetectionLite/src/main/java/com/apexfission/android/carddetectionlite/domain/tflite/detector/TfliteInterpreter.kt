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
 * ## TfliteInterpreter
 * A high-performance wrapper for the TensorFlow Lite [Interpreter] specialized for object detection.
 * * ### Key Responsibilities:
 * * **Model Initialization**: Automatically probes the model to detect input shapes, quantization parameters,
 * and output layouts (YOLO-style `[85 x 8400]` vs `[8400 x 85]`).
 * * **Memory Management**: Uses direct [ByteBuffer]s to minimize GC pressure during high-frequency inference.
 * * **Hardware Acceleration**: Configures [GpuDelegate] automatically for FP32 models on supported devices.
 * * **Quantization Handling**: Seamlessly bridges [Bitmap] (ARGB_8888) data to both **INT8** and **FP32** * tensors using optimized bit-shifting loops.
 *
 * @param context Android context used to access the assets folder.
 * @param modelPath The relative path to the `.tflite` model file in assets.
 * @param useGpu If true, attempts to initialize the GPU delegate (automatically ignored for INT8 models).
 * @param numThreads CPU thread count for inference. Defaults to half of available cores (min 2).
 */
class TfliteInterpreter(
    context: Context,
    modelPath: String,
    useGpu: Boolean,
    numThreads: Int?
) : Closeable {

    companion object {
        private const val TAG = "TfliteInterpreter"
    }

    private var interpreter: Interpreter
    private var gpuDelegate: GpuDelegate? = null
    private var isClosed = false

    /** Direct buffer for model input. Size adjusted based on [isInt8] data type. */
    private val inputBuffer: ByteBuffer
    /** Reusable array to hold bitmap pixels, avoiding per-frame allocations. */
    private val pixelBuffer: IntArray

    private val outCount: Int
    private val outByteBuffer: ByteBuffer
    private val outFloats: FloatArray

    /** Input quantization parameters. */
    private val inScale: Float
    private val inZeroPoint: Int

    /** Output quantization parameters for de-quantizing results. */
    private val outScale: Float
    private val outZeroPoint: Int

    /** Whether the loaded model uses INT8 quantization. */
    val isInt8: Boolean

    /** The required dimension (width/height) of the input image. */
    val inputImageWidth: Int

    /**
     * The detected layout of the output tensor.
     * Essential for correctly indexing boxes vs. class probabilities.
     */
    enum class OutputLayout {
        /** Format: [Attributes x Boxes] (e.g., 85 x 8400) */
        ATTRS_X_BOXES,
        /** Format: [Boxes x Attributes] (e.g., 8400 x 85) */
        BOXES_X_ATTRS
    }

    val outLayout: OutputLayout
    val outAttrs: Int
    val outBoxes: Int
    val numClasses: Int

    /** Duration of the most recent inference run in milliseconds. */
    var lastInferenceTimeMs: Long = 0L
        private set

    init {
        val modelBuffer = loadModelFile(context, modelPath)

        // 1. Probe Metadata: Use a temporary interpreter to determine tensor shapes
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

        Log.i(TAG, "Config: $numClasses classes, $outBoxes boxes, Layout: $outLayout, Int8: $isInt8")

        // 2. Configure Final Interpreter
        pixelBuffer = IntArray(inputImageWidth * inputImageWidth)
        val options = Interpreter.Options().apply {
            setNumThreads(numThreads ?: max(2, Runtime.getRuntime().availableProcessors() / 2))
        }

        // Apply GPU acceleration if requested and supported (primarily for FP32)
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

        // 3. Cache Quantization Params
        val inQ = interpreter.getInputTensor(0).quantizationParams()
        inScale = inQ.scale
        inZeroPoint = inQ.zeroPoint

        val outQ = interpreter.getOutputTensor(0).quantizationParams()
        outScale = outQ.scale
        outZeroPoint = outQ.zeroPoint

        // 4. Allocate Buffers
        val bytesPerChannel = if (isInt8) 1 else 4
        inputBuffer = ByteBuffer.allocateDirect(1 * inputImageWidth * inputImageWidth * 3 * bytesPerChannel).order(ByteOrder.nativeOrder())
        outByteBuffer = ByteBuffer.allocateDirect(outCount * bytesPerChannel).order(ByteOrder.nativeOrder())
        outFloats = FloatArray(outCount)
    }

    /**
     * Executes inference on the provided [Bitmap].
     * * Handles color channel extraction, normalization, and (if needed) quantization.
     * * Performs de-quantization on the output before returning.
     *
     * @param bitmap The image to analyze. Should match [inputImageWidth] for best performance.
     * @return Flattened [FloatArray] of detections.
     */
    @Synchronized
    fun runInference(bitmap: Bitmap): FloatArray {
        if (isClosed) return FloatArray(0)
        val startTime = SystemClock.uptimeMillis()

        if (isInt8) fillBitmapToByteBuffer(bitmap, inputBuffer)
        else fillBitmapToFloatBuffer(bitmap, inputBuffer)

        outByteBuffer.rewind()
        interpreter.run(inputBuffer, outByteBuffer)

        // Post-processing: Extract and de-quantize if necessary
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

    /** Normalizes pixels to [0.0, 1.0] for FP32 models. */
    private fun fillBitmapToFloatBuffer(bm: Bitmap, buf: ByteBuffer) {
        buf.rewind()
        bm.getPixels(pixelBuffer, 0, inputImageWidth, 0, 0, inputImageWidth, inputImageWidth)

        val inv255 = 1.0f / 255.0f
        for (v in pixelBuffer) {
            buf.putFloat(((v shr 16) and 0xFF) * inv255) // R
            buf.putFloat(((v shr 8) and 0xFF) * inv255)  // G
            buf.putFloat((v and 0xFF) * inv255)         // B
        }
    }

    /** Normalizes and quantizes pixels for INT8 models. */
    private fun fillBitmapToByteBuffer(bm: Bitmap, buf: ByteBuffer) {
        buf.rewind()
        bm.getPixels(pixelBuffer, 0, inputImageWidth, 0, 0, inputImageWidth, inputImageWidth)

        // Combined constant: (1/255) for normalization * (1/scale) for quantization
        val invScale = if (inScale != 0f) 1f / inScale else 1f
        val combinedMultiplier = (1.0f / 255.0f) * invScale

        for (v in pixelBuffer) {
            buf.put(quantizeToInt8(((v shr 16) and 0xFF) * combinedMultiplier, inZeroPoint))
            buf.put(quantizeToInt8(((v shr 8) and 0xFF) * combinedMultiplier, inZeroPoint))
            buf.put(quantizeToInt8((v and 0xFF) * combinedMultiplier, inZeroPoint))
        }
    }

    private fun quantizeToInt8(v: Float, zeroPoint: Int): Byte =
        (v + zeroPoint).toInt().coerceIn(-128, 127).toByte()

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