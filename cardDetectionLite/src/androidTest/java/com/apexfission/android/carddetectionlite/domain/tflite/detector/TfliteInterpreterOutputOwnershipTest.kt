package com.apexfission.android.carddetectionlite.domain.tflite.detector

import android.content.Context
import android.graphics.Bitmap
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.apexfission.android.carddetectionlite.domain.ModelCatalog
import com.apexfission.android.carddetectionlite.tfmodel.modelPath
import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import org.junit.Assume.assumeTrue
import org.tensorflow.lite.gpu.CompatibilityList
import org.junit.Assert
import org.junit.Assert.assertNotSame
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TfliteInterpreterOutputOwnershipTest {

    @Test
    fun subsequentInferenceReturnsAnIndependentOutputArray() {
        val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
        val interpreter = TfliteInterpreter(
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
    fun engineThreadAffinityIsPreservedAcrossLifecycle() = verifyLifecycle(useGpu = false)

    @Test
    fun gpuThreadAffinityIsPreservedAcrossLifecycle() {
        val supported = CompatibilityList().use { it.isDelegateSupportedOnThisDevice }
        assumeTrue("Requires a GPU-compatible device", supported)
        verifyLifecycle(useGpu = true)
    }

    private fun verifyLifecycle(useGpu: Boolean) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val interpreter = TfliteInterpreter(context, ModelCatalog.TfLite.modelPath, useGpu)
        val bitmap = Bitmap.createBitmap(
            interpreter.inputImageWidth, interpreter.inputImageWidth, Bitmap.Config.ARGB_8888,
        )
        val callers = Executors.newFixedThreadPool(5)
        try {
            if (useGpu) {
                // Ensure this is a GPU test, not a silently selected CPU path.
                val delegateField = TfliteInterpreter::class.java.getDeclaredField("gpuDelegate")
                delegateField.isAccessible = true
                Assert.assertNotNull("GPU delegate must be selected", delegateField.get(interpreter))
            }
            val engineThreadId = interpreter.engineThreadId
            Assert.assertTrue(engineThreadId != -1L)
            val results = List(5) {
                callers.submit(Callable {
                    Assert.assertTrue(interpreter.runInference(bitmap).isNotEmpty())
                    interpreter.lastInferenceThreadId
                })
            }
            results.forEach {
                Assert.assertEquals(engineThreadId, it.get(30, TimeUnit.SECONDS))
            }
            interpreter.close()
            Assert.assertEquals(engineThreadId, interpreter.closeThreadId)
            Assert.assertTrue(interpreter.runInference(bitmap).isEmpty())
        } finally {
            callers.shutdownNow()
            interpreter.close()
            bitmap.recycle()
        }
    }

    @Test
    fun interruptedCallerCompletesInferenceAndCloseWithoutAbandoningResources() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val interpreter = TfliteInterpreter(context, ModelCatalog.TfLite.modelPath, false)
        val bitmap = Bitmap.createBitmap(
            interpreter.inputImageWidth, interpreter.inputImageWidth, Bitmap.Config.ARGB_8888,
        )
        val caller = Executors.newSingleThreadExecutor()
        try {
            caller.submit(Callable {
                Thread.currentThread().interrupt()
                try {
                    Assert.assertTrue(interpreter.runInference(bitmap).isNotEmpty())
                    Assert.assertTrue(Thread.currentThread().isInterrupted)
                    interpreter.close()
                    Assert.assertEquals(interpreter.engineThreadId, interpreter.closeThreadId)
                    Assert.assertTrue(Thread.currentThread().isInterrupted)
                } finally {
                    Thread.interrupted()
                }
            }).get(30, TimeUnit.SECONDS)
        } finally {
            caller.shutdownNow()
            interpreter.close()
            bitmap.recycle()
        }
    }

    @Test
    fun concurrentCloseAndInferenceCompleteWithoutSubmissionFailures() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val interpreter = TfliteInterpreter(context, ModelCatalog.TfLite.modelPath, false)
        val bitmap = Bitmap.createBitmap(
            interpreter.inputImageWidth, interpreter.inputImageWidth, Bitmap.Config.ARGB_8888,
        )
        val callers = Executors.newFixedThreadPool(6)
        val start = CountDownLatch(1)
        try {
            val results = List(6) { index ->
                callers.submit(Callable {
                    check(start.await(10, TimeUnit.SECONDS))
                    if (index % 2 == 0) interpreter.close()
                    else interpreter.runInference(bitmap)
                })
            }
            start.countDown()
            results.forEach { it.get(30, TimeUnit.SECONDS) }
            Assert.assertEquals(interpreter.engineThreadId, interpreter.closeThreadId)
            Assert.assertTrue(interpreter.runInference(bitmap).isEmpty())
        } finally {
            start.countDown()
            callers.shutdownNow()
            interpreter.close()
            bitmap.recycle()
        }
    }
}
