package com.apexfission.android.carddetectionlite.domain.tflite.filters

import android.graphics.Bitmap
import com.apexfission.android.carddetectionlite.domain.coordinates.models.ImageBox
import com.apexfission.android.carddetectionlite.domain.tflite.model.Detection
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import org.mockito.Mockito.`when`

class MarginValidatorTest {

    private val mockBitmap: Bitmap = Mockito.mock(Bitmap::class.java)

    @Before
    fun setUp() {
        `when`(mockBitmap.width).thenReturn(1000)
        `when`(mockBitmap.height).thenReturn(1000)
    }

    @Test
    fun testValidBoxInsideMargin() {
        val validator = MarginValidator(margin = 20u)
        // Box coordinates: x=50, y=50, x2=900, y2=900 (all >= 20 and <= 980)
        val box = ImageBox.from2P(50U, 50U, 900U, 900U)
        val detection = Detection(box = box, confidence = 0.9f, classId = 0)

        assertTrue(validator.isValid(detection, null, mockBitmap))
    }

    @Test
    fun testBoxViolatesLeftMarginReturnsFalse() {
        val validator = MarginValidator(margin = 20u)
        // x = 10 (< 20 margin)
        val box = ImageBox.from2P(10U, 50U, 900U, 900U)
        val detection = Detection(box = box, confidence = 0.9f, classId = 0)

        assertFalse(validator.isValid(detection, null, mockBitmap))
    }

    @Test
    fun testBoxViolatesTopMarginReturnsFalse() {
        val validator = MarginValidator(margin = 20u)
        // y = 10 (< 20 margin)
        val box = ImageBox.from2P(50U, 10U, 900U, 900U)
        val detection = Detection(box = box, confidence = 0.9f, classId = 0)

        assertFalse(validator.isValid(detection, null, mockBitmap))
    }

    @Test
    fun testBoxViolatesRightMarginReturnsFalse() {
        val validator = MarginValidator(margin = 20u)
        // x2 = 990 (> 1000 - 20 = 980 margin)
        val box = ImageBox.from2P(50U, 50U, 990U, 900U)
        val detection = Detection(box = box, confidence = 0.9f, classId = 0)

        assertFalse(validator.isValid(detection, null, mockBitmap))
    }

    @Test
    fun testBoxViolatesBottomMarginReturnsFalse() {
        val validator = MarginValidator(margin = 20u)
        // y2 = 990 (> 1000 - 20 = 980 margin)
        val box = ImageBox.from2P(50U, 50U, 900U, 990U)
        val detection = Detection(box = box, confidence = 0.9f, classId = 0)

        assertFalse(validator.isValid(detection, null, mockBitmap))
    }

    @Test
    fun testExactBoundaryMarginReturnsTrue() {
        val validator = MarginValidator(margin = 20u)
        // x=20, y=20, x2=980, y2=980
        val box = ImageBox.from2P(20U, 20U, 980U, 980U)
        val detection = Detection(box = box, confidence = 0.9f, classId = 0)

        assertTrue(validator.isValid(detection, null, mockBitmap))
    }

    @Test
    fun testMarginExceedsHalfBitmapDimensionsReturnsFalse() {
        val smallBitmap: Bitmap = Mockito.mock(Bitmap::class.java)
        `when`(smallBitmap.width).thenReturn(30)
        `when`(smallBitmap.height).thenReturn(30)

        val validator = MarginValidator(margin = 20u) // 20 * 2 = 40 >= 30
        val box = ImageBox.from2P(5U, 5U, 25U, 25U)
        val detection = Detection(box = box, confidence = 0.9f, classId = 0)

        assertFalse(validator.isValid(detection, null, smallBitmap))
    }

    @Test
    fun testCustomMarginValue() {
        val validator = MarginValidator(margin = 50u)
        // x = 40 (< 50 margin)
        val box = ImageBox.from2P(40U, 60U, 900U, 900U)
        val detection = Detection(box = box, confidence = 0.9f, classId = 0)

        assertFalse(validator.isValid(detection, null, mockBitmap))
    }
}
