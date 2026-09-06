package com.apexfission.android.carddetectionlite.domain.tflite.filters

import android.graphics.Bitmap
import com.apexfission.android.carddetectionlite.domain.coordinates.models.ImageBox
import com.apexfission.android.carddetectionlite.domain.tflite.model.Detection
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito

class AspectRatioValidatorTest {

    private val dummyBitmap: Bitmap = Mockito.mock(Bitmap::class.java)

    @Test
    fun testValidHorizontalCardAspectRatio() {
        val validator = AspectRatioValidator()
        // Width = 150, Height = 100 -> Aspect Ratio = 1.5 (within 1.28..1.70)
        val box = ImageBox.from2P(0U, 0U, 150U, 100U)
        val detection = Detection(box = box, confidence = 0.9f, classId = 0)

        assertTrue(validator.isValid(detection, null, dummyBitmap))
    }

    @Test
    fun testValidVerticalCardAspectRatio() {
        val validator = AspectRatioValidator()
        // Width = 100, Height = 150 -> Aspect Ratio = 1.5 (within 1.28..1.70)
        val box = ImageBox.from2P(0U, 0U, 100U, 150U)
        val detection = Detection(box = box, confidence = 0.9f, classId = 0)

        assertTrue(validator.isValid(detection, null, dummyBitmap))
    }

    @Test
    fun testAspectRatioBelowMinThresholdReturnsFalse() {
        val validator = AspectRatioValidator(minAspectRatio = 1.28f, maxAspectRatio = 1.7f)
        // Width = 100, Height = 100 -> Aspect Ratio = 1.0 (< 1.28)
        val box = ImageBox.from2P(0U, 0U, 100U, 100U)
        val detection = Detection(box = box, confidence = 0.9f, classId = 0)

        assertFalse(validator.isValid(detection, null, dummyBitmap))
    }

    @Test
    fun testAspectRatioAboveMaxThresholdReturnsFalse() {
        val validator = AspectRatioValidator(minAspectRatio = 1.28f, maxAspectRatio = 1.7f)
        // Width = 200, Height = 100 -> Aspect Ratio = 2.0 (> 1.7)
        val box = ImageBox.from2P(0U, 0U, 200U, 100U)
        val detection = Detection(box = box, confidence = 0.9f, classId = 0)

        assertFalse(validator.isValid(detection, null, dummyBitmap))
    }

    @Test
    fun testExactBoundaryMinAspectRatioReturnsTrue() {
        val validator = AspectRatioValidator(minAspectRatio = 1.28f, maxAspectRatio = 1.7f)
        // Width = 128, Height = 100 -> Aspect Ratio = 1.28
        val box = ImageBox.from2P(0U, 0U, 128U, 100U)
        val detection = Detection(box = box, confidence = 0.9f, classId = 0)

        assertTrue(validator.isValid(detection, null, dummyBitmap))
    }

    @Test
    fun testExactBoundaryMaxAspectRatioReturnsTrue() {
        val validator = AspectRatioValidator(minAspectRatio = 1.28f, maxAspectRatio = 1.7f)
        // Width = 170, Height = 100 -> Aspect Ratio = 1.70
        val box = ImageBox.from2P(0U, 0U, 170U, 100U)
        val detection = Detection(box = box, confidence = 0.9f, classId = 0)

        assertTrue(validator.isValid(detection, null, dummyBitmap))
    }

    @Test
    fun testZeroWidthReturnsFalse() {
        val validator = AspectRatioValidator()
        val box = ImageBox.from2P(100U, 100U, 100U, 200U)
        val detection = Detection(box = box, confidence = 0.9f, classId = 0)

        assertFalse(validator.isValid(detection, null, dummyBitmap))
    }

    @Test
    fun testZeroHeightReturnsFalse() {
        val validator = AspectRatioValidator()
        val box = ImageBox.from2P(100U, 100U, 200U, 100U)
        val detection = Detection(box = box, confidence = 0.9f, classId = 0)

        assertFalse(validator.isValid(detection, null, dummyBitmap))
    }

    @Test
    fun testCustomAspectRatioThresholds() {
        val validator = AspectRatioValidator(minAspectRatio = 1.0f, maxAspectRatio = 2.5f)
        // Square box (Ratio = 1.0)
        val box = ImageBox.from2P(0U, 0U, 100U, 100U)
        val detection = Detection(box = box, confidence = 0.9f, classId = 0)

        assertTrue(validator.isValid(detection, null, dummyBitmap))
    }
}
