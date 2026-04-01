package com.apexfission.android.carddetectionlite.domain.tflite.detector

import android.content.Context
import android.graphics.Bitmap
import android.os.SystemClock
import android.util.Log
import com.apexfission.android.carddetectionlite.ui.NumThreads
import java.io.Closeable
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.CompatibilityList
import org.tensorflow.lite.gpu.GpuDelegate

/**
 * A high-performance wrapper for the TensorFlow Lite [Interpreter], specialized for object detection models.
 *
 * This class abstracts away the complexities of interacting with the TFLite API.
 *
 * ### Key Responsibilities:
 * - **Model Initialization**: Automatically probes the model file to determine critical metadata like
 *   input shapes, quantization parameters, and the specific output layout of YOLO-style models.
 * - **Memory Management**: Pre-allocates and reuses direct `ByteBuffer`s for model inputs and outputs,
 *   minimizing garbage collection overhead during high-frequency inference (e.g., from a camera stream).
 * - **Hardware Acceleration**: Automatically configures and enables the [GpuDelegate] for float-based (FP32)
 *   models on supported devices to significantly improve performance.
 * - **Quantization Handling**: Seamlessly handles both `INT8` and `FP32` models. It correctly quantizes
 *   input `Bitmap` data and de-quantizes output results when required.
 *
 * @param context The Android `Context`, used to access the assets folder where the model is stored.
 * @param modelPath The relative path from the `assets` directory to the `.tflite` model file.
 * @param useGpu If `true`, the interpreter will attempt to use the GPU delegate for acceleration.
 *               Note: This is typically only effective for non-quantized (FP32) models.
 * @param numThreads The number of CPU threads to be used for inference in case of not using the gpu.
 *                  If `null`, a sensible default is chosen (half of the available processor cores,
 *                  with a minimum of 2).
 */
class TfliteInterpreter(
    context: Context,
    modelPath: String,
    useGpu: Boolean,
    numThreads: NumThreads = NumThreads.Default
) : Closeable {

    companion object {
        private const val TAG = "TfliteInterpreter"
    }

    private var interpreter: Interpreter
    private var gpuDelegate: GpuDelegate? = null
    private var isClosed = false

    /** Direct buffer for model input. Its size is adjusted based on the model's data type ([isInt8]). */
    private val inputBuffer: ByteBuffer

    /** A reusable array to hold raw pixel data from the input bitmap, avoiding per-frame allocations. */
    private val pixelBuffer: IntArray

    private val outCount: Int
    private val outByteBuffer: ByteBuffer
    private val outFloats: FloatArray

    private val inScale: Float
    private val inZeroPoint: Int
    private val outScale: Float
    private val outZeroPoint: Int

    /** `true` if the loaded model uses INT8 quantization, `false` if it uses FP32. */
    val isInt8: Boolean

    /** The required width and height of the square input image for the model (e.g., 640 for a 640x640 model). */
    val inputImageWidth: Int

    /**
     * Represents the dimensional layout of a YOLO model's output tensor. This is crucial for
     * correctly parsing the detection results.
     */
    enum class OutputLayout {
        /** Tensor shape is `[Attributes x Boxes]` (e.g., 85 attributes, 8400 boxes). */
        ATTRS_X_BOXES,

        /** Tensor shape is `[Boxes x Attributes]` (e.g., 8400 boxes, 85 attributes). */
        BOXES_X_ATTRS
    }

    /** The detected output layout of the loaded model. */
    val outLayout: OutputLayout

    /** The number of attributes per detection (e.g., 4 for box coords + 80 for classes + 1 for confidence = 85). */
    val outAttrs: Int

    /** The total number of bounding boxes the model can predict in a single inference. */
    val outBoxes: Int

    /** The number of distinct object classes the model can identify. */
    val numClasses: Int

    /** The execution time of the most recent `runInference` call in milliseconds. */
    var lastInferenceTimeMs: Long = 0L
        private set

    init {
        val modelBuffer = loadModelFile(context, modelPath)

        // 1. Probe Metadata: Use a temporary interpreter to determine tensor shapes and properties.
        val temp = Interpreter(modelBuffer)
        val inputTensor = temp.getInputTensor(0)
        val outputTensor = temp.getOutputTensor(0)

        isInt8 = inputTensor.dataType() == DataType.INT8
        inputImageWidth = inputTensor.shape()[1]

        val oShape = outputTensor.shape()
        outCount = oShape.reduce { a, b -> a * b }

        // Heuristic to detect the output layout based on typical YOLO dimensions.
        val dim1 = oShape[1]
        val dim2 = oShape[2]
        outLayout = if (dim2 >= 400 && dim1 <= 256) OutputLayout.ATTRS_X_BOXES else OutputLayout.BOXES_X_ATTRS
        outAttrs = if (outLayout == OutputLayout.ATTRS_X_BOXES) dim1 else dim2
        outBoxes = if (outLayout == OutputLayout.ATTRS_X_BOXES) dim2 else dim1
        numClasses = outAttrs - 4

        temp.close()

        Log.i(TAG, "Config: $numClasses classes, $outBoxes boxes, Layout: $outLayout, Int8: $isInt8")

        // 2. Configure Final Interpreter with appropriate options.
        pixelBuffer = IntArray(inputImageWidth * inputImageWidth)
        val options = Interpreter.Options().apply {
            setNumThreads(numThreads.toInt())
        }

        if (useGpu && !isInt8) {
            try {
                val compat = CompatibilityList()
                if (compat.isDelegateSupportedOnThisDevice) {
                    gpuDelegate = GpuDelegate(compat.bestOptionsForThisDevice)
                    options.addDelegate(gpuDelegate)
                }
            } catch (_: Throwable) {
                gpuDelegate = null
            }
        }

        interpreter = Interpreter(modelBuffer, options)

        // 3. Cache Quantization Parameters for later use in data conversion.
        val inQ = interpreter.getInputTensor(0).quantizationParams()
        inScale = inQ.scale
        inZeroPoint = inQ.zeroPoint

        val outQ = interpreter.getOutputTensor(0).quantizationParams()
        outScale = outQ.scale
        outZeroPoint = outQ.zeroPoint

        // 4. Allocate I/O Buffers.
        val bytesPerChannel = if (isInt8) 1 else 4
        inputBuffer = ByteBuffer.allocateDirect(1 * inputImageWidth * inputImageWidth * 3 * bytesPerChannel).order(ByteOrder.nativeOrder())
        outByteBuffer = ByteBuffer.allocateDirect(outCount * bytesPerChannel).order(ByteOrder.nativeOrder())
        outFloats = FloatArray(outCount)
    }

    /**
     * Executes the TFLite model inference on the provided [Bitmap].
     *
     * This method handles the full pipeline: it preprocesses the bitmap data into the
     * correct format (FP32 or INT8), feeds it to the interpreter, runs inference,
     * and de-quantizes the output if necessary, returning a clean `FloatArray`.
     *
     * @param bitmap The input image. For best performance, it should already be scaled
     *               to the model's required dimensions ([inputImageWidth] x [inputImageWidth]).
     * @return A flattened `FloatArray` containing the raw output of the model.
     */
    @Synchronized
    fun runInference(bitmap: Bitmap): FloatArray {
        if (isClosed) return FloatArray(0)
        val startTime = SystemClock.uptimeMillis()

        // Fill the input buffer based on whether the model is quantized or not.
        if (isInt8) fillBitmapToByteBuffer(bitmap, inputBuffer)
        else fillBitmapToFloatBuffer(bitmap, inputBuffer)

        outByteBuffer.rewind()
        interpreter.run(inputBuffer, outByteBuffer)

        // Extract results and de-quantize if necessary.
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

    /** Prepares bitmap data for an FP32 model by normalizing pixel values to the [0.0, 1.0] range. */
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

    /** Prepares bitmap data for an INT8 model by normalizing and then quantizing pixel values. */
    private fun fillBitmapToByteBuffer(bm: Bitmap, buf: ByteBuffer) {
        buf.rewind()
        bm.getPixels(pixelBuffer, 0, inputImageWidth, 0, 0, inputImageWidth, inputImageWidth)
        val invScale = if (inScale != 0f) 1f / inScale else 1f
        val combinedMultiplier = (1.0f / 255.0f) * invScale
        for (v in pixelBuffer) {
            buf.put(quantizeToInt8(((v shr 16) and 0xFF) * combinedMultiplier, inZeroPoint))
            buf.put(quantizeToInt8(((v shr 8) and 0xFF) * combinedMultiplier, inZeroPoint))
            buf.put(quantizeToInt8((v and 0xFF) * combinedMultiplier, inZeroPoint))
        }
    }

    /** Applies the quantization formula to a single float value. */
    private fun quantizeToInt8(v: Float, zeroPoint: Int): Byte =
        (v + zeroPoint).toInt().coerceIn(-128, 127).toByte()

    /** Loads the TFLite model from assets into a direct ByteBuffer. */
    private fun loadModelFile(context: Context, assetPath: String): ByteBuffer {
        val fd = context.assets.openFd(assetPath)
        return FileInputStream(fd.fileDescriptor).channel.map(
            FileChannel.MapMode.READ_ONLY, fd.startOffset, fd.declaredLength
        ).order(ByteOrder.nativeOrder())
    }

    /**
     * Closes the TFLite interpreter and releases associated resources like the GPU delegate.
     * This is crucial to call to prevent memory leaks.
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
