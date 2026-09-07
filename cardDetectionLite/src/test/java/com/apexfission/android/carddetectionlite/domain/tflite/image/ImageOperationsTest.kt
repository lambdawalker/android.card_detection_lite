package com.apexfission.android.carddetectionlite.domain.tflite.image

import androidx.compose.ui.unit.dp
import com.apexfission.android.carddetectionlite.domain.tflite.detector.InputShape
import org.junit.Assert.assertEquals
import org.junit.Test

class ImageOperationsTest {

    @Test
    fun testInputShapeOptions() {
        val centerCrop = InputShape.CenterSquareCrop
        val offsetCrop = InputShape.SquareCrop(top = 50.dp)
        assertEquals(50.dp, offsetCrop.top)
    }
}
