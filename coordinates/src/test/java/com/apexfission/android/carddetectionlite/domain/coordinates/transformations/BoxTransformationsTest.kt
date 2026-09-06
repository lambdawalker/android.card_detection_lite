package com.apexfission.android.carddetectionlite.domain.coordinates.transformations

import com.apexfission.android.carddetectionlite.domain.coordinates.models.ImageBox
import com.apexfission.android.carddetectionlite.domain.coordinates.models.ImageSpace
import org.junit.Assert.assertEquals
import org.junit.Test

class BoxTransformationsTest {

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
    fun testImageBoxToParentSpace() {
        val childBox = ImageBox.from2P(x1 = 20U, y1 = 20U, x2 = 40U, y2 = 40U)
        val expected = ImageBox.from2P(x1 = 110U, y1 = 110U, x2 = 120U, y2 = 120U)


        val result2 = childBox.toParentSpace(parentSpace, childSpace)
        assertEquals(expected, result2)
    }

    @Test
    fun testImageBoxToChildSpace() {
        val parentBox = ImageBox.from2P(x1 = 110U, y1 = 110U, x2 = 120U, y2 = 120U)
        val expected = ImageBox.from2P(x1 = 20U, y1 = 20U, x2 = 40U, y2 = 40U)


        val result2 = parentBox.toChildSpace(parentSpace, childSpace)
        assertEquals(expected, result2)
    }
}
