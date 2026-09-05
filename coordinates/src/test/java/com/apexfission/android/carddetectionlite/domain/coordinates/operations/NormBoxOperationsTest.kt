package com.apexfission.android.carddetectionlite.domain.coordinates.operations

import com.apexfission.android.carddetectionlite.domain.coordinates.ImageBox2P
import com.apexfission.android.carddetectionlite.domain.coordinates.ImageBoxPS
import com.apexfission.android.carddetectionlite.domain.coordinates.ImageSpace
import com.apexfission.android.carddetectionlite.domain.coordinates.NormImageBox2P
import com.apexfission.android.carddetectionlite.domain.coordinates.NormImageBoxPS
import org.junit.Assert.assertEquals
import org.junit.Test

class NormBoxOperationsTest {

    private val localSpace = ImageSpace(width = 1000U, height = 500U)

    @Test
    fun testNormBox2PToBox2P() {
        val normBox = NormImageBox2P(x = 0.1F, y = 0.2F, x2 = 0.5F, y2 = 0.8F)
        val expected = ImageBox2P(x = 100U, y = 100U, x2 = 500U, y2 = 400U)

        assertEquals(expected, normBox2PToBox2P(normBox, localSpace))
        assertEquals(expected, normBox.toBox2P(localSpace))
    }

    @Test
    fun testNormBoxPStoBoxPS() {
        val normBoxPS = NormImageBoxPS(x = 0.2F, y = 0.1F, width = 0.5F, height = 0.4F)
        val expected = ImageBoxPS(x = 200U, y = 50U, width = 500, height = 200)

        assertEquals(expected, normBoxPStoBoxPS(normBoxPS, localSpace))
        assertEquals(expected, normBoxPS.toBoxPS(localSpace))
    }
}
