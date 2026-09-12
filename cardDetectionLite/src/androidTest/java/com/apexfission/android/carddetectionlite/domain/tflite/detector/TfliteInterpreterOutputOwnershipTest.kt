package com.apexfission.android.carddetectionlite.domain.tflite.detector

import android.content.Context
import android.graphics.Bitmap
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.apexfission.android.carddetectionlite.domain.ModelCatalog
import com.apexfission.android.carddetectionlite.tfmodel.modelPath
import java.util.Collections
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
    fun engineThreadAffinityIsPreservedAcrossLifecycle() {
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
            val engineThreadId = interpreter.engineThreadId
            Assert.assertTrue("Engine thread ID must be valid", engineThreadId != -1L)

            val recordedInferenceThreadIds = Collections.synchronizedList(mutableListOf<Long>())

            // Execute inferences from multiple different caller threads
            val threads = List(5) {
                Thread {
                    interpreter.runInference(bitmap)
                    recordedInferenceThreadIds.add(interpreter.lastInferenceThreadId)
                }
            }
            threads.forEach { it.start() }
            threads.forEach { it.join() }

            for (threadId in recordedInferenceThreadIds) {
                Assert.assertEquals(
                    "All inferences must execute on the dedicated engine thread",
                    engineThreadId,
                    threadId,
                )
            }

            interpreter.close()
            Assert.assertEquals(
                "Teardown must execute on the dedicated engine thread",
                engineThreadId,
                interpreter.closeThreadId,
            )
        } finally {
            bitmap.recycle()
            interpreter.close()
        }
    }
}
