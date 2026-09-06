package com.apexfission.android.carddetectionlite.domain.tflite.filters

import android.graphics.Bitmap
import com.apexfission.android.carddetectionlite.domain.coordinates.models.ImageBox
import com.apexfission.android.carddetectionlite.domain.tflite.model.Detection
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito

class CardValidatorTest {

    private val dummyBitmap: Bitmap = Mockito.mock(Bitmap::class.java)

    @Test
    fun testCardValidatorFunctionalInterfaceLambda() {
        // Custom validator that checks if confidence is >= 0.8f
        val confidenceValidator = CardValidator { detection, _, _ ->
            detection.confidence >= 0.8f
        }

        val highConfidenceDetection = Detection(
            box = ImageBox.from2P(0U, 0U, 100U, 100U),
            confidence = 0.85f,
            classId = 0
        )
        val lowConfidenceDetection = Detection(
            box = ImageBox.from2P(0U, 0U, 100U, 100U),
            confidence = 0.50f,
            classId = 0
        )

        assertTrue(confidenceValidator.isValid(highConfidenceDetection, null, dummyBitmap))
        assertFalse(confidenceValidator.isValid(lowConfidenceDetection, null, dummyBitmap))
    }

    @Test
    fun testCardValidatorCompositionAllMustPass() {
        val validators: List<CardValidator> = listOf(
            CardValidator { detection, _, _ -> detection.confidence >= 0.8f },
            CardValidator { detection, _, _ -> detection.classId == 0 }
        )

        val matchingDetection = Detection(
            box = ImageBox.from2P(0U, 0U, 100U, 100U),
            confidence = 0.9f,
            classId = 0
        )
        val wrongClassDetection = Detection(
            box = ImageBox.from2P(0U, 0U, 100U, 100U),
            confidence = 0.9f,
            classId = 1
        )

        assertTrue(validators.all { it.isValid(matchingDetection, null, dummyBitmap) })
        assertFalse(validators.all { it.isValid(wrongClassDetection, null, dummyBitmap) })
    }
}
