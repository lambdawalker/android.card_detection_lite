package com.apexfission.android.carddetectionlite.domain.coordinates.transformations

import com.apexfission.android.carddetectionlite.domain.coordinates.models.ImageBox
import com.apexfission.android.carddetectionlite.domain.coordinates.models.ImageSpace
import com.apexfission.android.carddetectionlite.domain.coordinates.models.NormImageBox
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
    fun testNormImageBoxFromPSToParentSpace() {
        val normBox = NormImageBox.fromPS(x = 0.05F, y = 0.05F, width = 0.05F, height = 0.05F)
        val expected = ImageBox.from2P(x1 = 110U, y1 = 110U, x2 = 120U, y2 = 120U)

        val result = normBox.toParentSpace(parentSpace, childSpace)
        assertEquals(expected, result)
    }

    @Test
    fun testNormImageBoxFrom2PToParentSpace() {
        val normBox = NormImageBox.from2P(x1 = 0.05F, y1 = 0.05F, x2 = 0.10F, y2 = 0.10F)
        val expected = ImageBox.from2P(x1 = 110U, y1 = 110U, x2 = 120U, y2 = 120U)

        val result = normBox.toParentSpace(parentSpace, childSpace)
        assertEquals(expected, result)
    }
}
