package com.apexfission.android.carddetectionlite.domain.tflite.image

import androidx.compose.ui.unit.dp
import com.apexfission.android.carddetectionlite.domain.tflite.detector.PreProcessingImageTransformation
import org.junit.Assert.assertEquals
import org.junit.Test

class ImageOperationsTest {

    @Test
    fun testInputShapeOptions() {
        val centerCrop = PreProcessingImageTransformation.CenterSquareCrop
        val offsetCrop = PreProcessingImageTransformation.SquareCrop(top = 50.dp)
        assertEquals(50.dp, offsetCrop.top)
    }
}
