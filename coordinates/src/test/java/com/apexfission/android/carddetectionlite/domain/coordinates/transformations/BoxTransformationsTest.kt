package com.apexfission.android.carddetectionlite.domain.coordinates.transformations

import com.apexfission.android.carddetectionlite.domain.coordinates.ImageBox2P
import com.apexfission.android.carddetectionlite.domain.coordinates.ImageSpace
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
    fun testImageBox2PToParentSpace() {
        val childBox = ImageBox2P(x = 20U, y = 20U, x2 = 40U, y2 = 40U)
        val expected = ImageBox2P(x = 110U, y = 110U, x2 = 120U, y2 = 120U)

        val result1 = imageBox2PToParentSpace(childBox, parentSpace, childSpace)
        assertEquals(expected, result1)

        val result2 = childBox.toParentSpace(parentSpace, childSpace)
        assertEquals(expected, result2)
    }

    @Test
    fun testImageBox2PToChildSpace() {
        val parentBox = ImageBox2P(x = 110U, y = 110U, x2 = 120U, y2 = 120U)
        val expected = ImageBox2P(x = 20U, y = 20U, x2 = 40U, y2 = 40U)

        val result1 = imageBox2PToChildSpace(parentBox, parentSpace, childSpace)
        assertEquals(expected, result1)

        val result2 = parentBox.toChildSpace(parentSpace, childSpace)
        assertEquals(expected, result2)
    }
}
