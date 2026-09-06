package com.apexfission.android.carddetectionlite.domain.coordinates.transformations

import com.apexfission.android.carddetectionlite.domain.coordinates.models.ImagePoint
import com.apexfission.android.carddetectionlite.domain.coordinates.models.ImageSpace
import com.apexfission.android.carddetectionlite.domain.coordinates.models.NormImagePoint
import org.junit.Assert.assertEquals
import org.junit.Test

class NormPointTransformationsTest {

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
    fun testNormPointToParentSpace() {
        val normPoint = NormImagePoint(x = 0.05F, y = 0.05F)
        val expected = ImagePoint(110U, 110U)

        val result = normPoint.toParentSpace(parentSpace, childSpace)
        assertEquals(expected, result)
    }

    @Test
    fun testNormPointToChildSpace() {
        val normPoint = NormImagePoint(x = 0.375F, y = 0.375F)
        val expected = ImagePoint(100U, 100U)

        val result = normPoint.toChildSpace(parentSpace, childSpace)
        assertEquals(expected, result)
    }
}
