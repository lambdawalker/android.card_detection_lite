package com.apexfission.android.carddetectionlite.domain.coordinates.transformations

import com.apexfission.android.carddetectionlite.domain.coordinates.ImageBox2P
import com.apexfission.android.carddetectionlite.domain.coordinates.ImageSpace
import com.apexfission.android.carddetectionlite.domain.coordinates.NormImageBox2P
import com.apexfission.android.carddetectionlite.domain.coordinates.NormImageBoxPS
import org.junit.Assert.assertEquals
import org.junit.Test

class NormBoxTransformationsTest {

    private val parentSpace = ImageSpace(width = 1000U, height = 1000U)
    private val childSpace = ImageSpace(
        width = 400U,
        height = 400U,
        xOffset = 100U,
        yOffset = 100U,
        xScale = 2.0F,
        yScale = 2.0F
    )

    @Test
    fun testNormImageBoxPSToParentSpace() {
        val normBoxPS = NormImageBoxPS(x = 0.05F, y = 0.05F, width = 0.05F, height = 0.05F)
        val expected = ImageBox2P(x = 110U, y = 110U, x2 = 120U, y2 = 120U)

        val result = normBoxPS.toParentSpace(normBoxPS, parentSpace, childSpace)
        assertEquals(expected, result)
    }

    @Test
    fun testNormImageBox2PToParentSpace() {
        val normBox2P = NormImageBox2P(x = 0.05F, y = 0.05F, x2 = 0.10F, y2 = 0.10F)
        val expected = ImageBox2P(x = 110U, y = 110U, x2 = 120U, y2 = 120U)

        val result = normBox2P.toParentSpace(normBox2P, parentSpace, childSpace)
        assertEquals(expected, result)
    }
}
