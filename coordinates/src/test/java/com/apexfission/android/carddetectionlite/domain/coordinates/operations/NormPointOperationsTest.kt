package com.apexfission.android.carddetectionlite.domain.coordinates.operations

import com.apexfission.android.carddetectionlite.domain.coordinates.ImagePoint
import com.apexfission.android.carddetectionlite.domain.coordinates.ImageSpace
import com.apexfission.android.carddetectionlite.domain.coordinates.NormImagePoint
import org.junit.Assert.assertEquals
import org.junit.Test

class NormPointOperationsTest {

    private val localSpace = ImageSpace(width = 1920U, height = 1080U)

    @Test
    fun testNormPointToPoint() {
        val normPoint = NormImagePoint(0.5F, 0.25F)
        val expected = ImagePoint(960U, 270U)

        assertEquals(expected, normPointToPoint(normPoint, localSpace))
        assertEquals(expected, normPoint.toPoint(localSpace))
    }
}
