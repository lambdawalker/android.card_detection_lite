package com.apexfission.android.carddetectionlite.domain.tflite.detector

import android.content.Context
import android.graphics.Bitmap
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.apexfission.android.carddetectionlite.domain.ModelCatalog
import com.apexfission.android.carddetectionlite.tfmodel.modelPath
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
}
