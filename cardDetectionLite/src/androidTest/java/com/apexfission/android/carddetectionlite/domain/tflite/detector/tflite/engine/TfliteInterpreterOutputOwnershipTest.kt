package com.apexfission.android.carddetectionlite.domain.tflite.detector.tflite.engine

import android.content.Context
import android.graphics.Bitmap
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.apexfission.android.carddetectionlite.domain.ModelCatalog
import com.apexfission.android.carddetectionlite.tfmodel.modelPath
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import org.junit.Assume.assumeTrue
import org.tensorflow.lite.gpu.CompatibilityList
import com.apexfission.android.carddetectionlite.domain.tflite.detector.yolo.engine.buildYoloDetector
import com.apexfission.android.carddetectionlite.domain.tflite.detector.card.engine.buildCardDetector
import org.junit.Assert
import org.junit.Assert.assertNotSame
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TfliteInterpreterOutputOwnershipTest {

    @Test
    fun subsequentInferenceReturnsAnIndependentOutputArray() {
        val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
        val interpreter = buildInferenceEngine(
            context = context,
            modelPath = ModelCatalog.TfLite.modelPath,
            useGpu = false,
        )
        val bitmap = Bitmap.createBitmap(
            interpreter.inputImageWidth,
            interpreter.inputImageWidth,
            Bitmap.Config.ARGB_8888,
        )

        try {
            val firstOutput = interpreter.runInference(bitmap)
            val secondOutput = interpreter.runInference(bitmap)

            assertNotSame(
                "Each inference result must own an independent output array",
                firstOutput,
                secondOutput,
            )
        } finally {
            bitmap.recycle()
            interpreter.close()
        }
    }

    @Test
    fun engineThreadAffinityIsPreservedAcrossLifecycle() = verifyLifecycle(false)

    @Test
    fun gpuAffinityIsPreservedAcrossLifecycle() {
        assumeTrue("Requires GPU-compatible hardware", CompatibilityList().use {
            it.isDelegateSupportedOnThisDevice
        })
        verifyLifecycle(true)
    }

    private fun verifyLifecycle(useGpu: Boolean) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        var selectedGpu = false
        val engine = ThreadConfinedInferenceEngine {
            InferenceCore(context, ModelCatalog.TfLite.modelPath, useGpu).also { core ->
                val field = InferenceCore::class.java.getDeclaredField("gpuDelegate")
                field.isAccessible = true
                selectedGpu = field.get(core) != null
            }
        }
        val bitmap = Bitmap.createBitmap(engine.inputImageWidth, engine.inputImageWidth, Bitmap.Config.ARGB_8888)
        val callers = Executors.newFixedThreadPool(5)
        try {
            if (useGpu) Assert.assertTrue("Must actually select a GPU delegate", selectedGpu)
            val expected = engine.engineThreadId
            Assert.assertTrue(expected != -1L)
            val results = List(5) { callers.submit(Callable {
                Assert.assertTrue(engine.runInference(bitmap).isNotEmpty())
                engine.lastInferenceThreadId
            }) }
            results.forEach { Assert.assertEquals(expected, it.get(30, TimeUnit.SECONDS)) }
            engine.close()
            Assert.assertEquals(expected, engine.closeThreadId)
            Assert.assertTrue(engine.runInference(bitmap).isEmpty())
        } finally {
            callers.shutdownNow()
            engine.close()
            bitmap.recycle()
        }
    }

    @Test(timeout = 60000)
    fun realBuildersComposeOnOneSharedContext() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        EngineThreadDispatcher().use { worker ->
            val yolo = buildYoloDetector(
                context, ModelCatalog.TfLite.modelPath, 0.5f, 0.45f, false,
                sharedDispatcher = worker,
            )
            try {
                val cards = buildCardDetector(yolo, cardClasses = setOf(0), sharedDispatcher = worker)
                try {
                    val bitmap = Bitmap.createBitmap(640, 640, Bitmap.Config.ARGB_8888)
                    try { cards.track(bitmap) } finally { bitmap.recycle() }
                } finally { cards.close() }
            } finally { yolo.close() }
        }
    }
}
