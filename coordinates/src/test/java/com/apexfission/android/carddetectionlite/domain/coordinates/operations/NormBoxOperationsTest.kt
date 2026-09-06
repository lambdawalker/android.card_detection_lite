package com.apexfission.android.carddetectionlite.domain.coordinates.operations

import com.apexfission.android.carddetectionlite.domain.coordinates.models.ImageBox
import com.apexfission.android.carddetectionlite.domain.coordinates.models.ImageSpace
import com.apexfission.android.carddetectionlite.domain.coordinates.models.NormImageBox
import org.junit.Assert.assertEquals
import org.junit.Test

class NormBoxOperationsTest {

    private val localSpace = ImageSpace(width = 1000U, height = 500U)

    @Test
    fun testNormBoxToBox() {
        val normBox = NormImageBox.from2P(x1 = 0.1F, y1 = 0.2F, x2 = 0.5F, y2 = 0.8F)
        val expected = ImageBox.from2P(x1 = 100U, y1 = 100U, x2 = 500U, y2 = 400U)

        assertEquals(expected, normBoxToBox(normBox, localSpace))
        assertEquals(expected, normBox.toBox(localSpace))
    }

    @Test
    fun testNormBoxPStoBox() {
        val normBoxPS = NormImageBox.fromPS(x = 0.2F, y = 0.1F, width = 0.5F, height = 0.4F)
        val expected = ImageBox.fromPS(x = 200U, y = 50U, width = 500U, height = 200U)

        assertEquals(expected, normBoxToBox(normBoxPS, localSpace))
        assertEquals(expected, normBoxPS.toBox(localSpace))
    }
}
