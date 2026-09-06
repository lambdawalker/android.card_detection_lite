package com.apexfission.android.carddetectionlite.domain.coordinates.transformations

import com.apexfission.android.carddetectionlite.domain.coordinates.models.ImagePoint
import com.apexfission.android.carddetectionlite.domain.coordinates.models.ImageSpace
import org.junit.Assert.assertEquals
import org.junit.Test

class PointTransformationsTest {

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
    fun testPointToParentSpaceWithCoordinates() {
        val expected = ImagePoint(110U, 110U)

        val result1 = pointToParentSpace(20U, 20U, childSpace, parentSpace)
        assertEquals(expected, result1)
    }

    @Test
    fun testPointToParentSpaceWithImagePoint() {
        val point = ImagePoint(20U, 20U)
        val expected = ImagePoint(110U, 110U)

        val result2 = point.toParentSpace(parentSpace, childSpace)
        assertEquals(expected, result2)
    }

    @Test
    fun testPointToChildSpaceWithCoordinates() {
        val expected = ImagePoint(100U, 100U)

        val result1 = pointToChildSpace(150U, 150U, parentSpace, childSpace)
        assertEquals(expected, result1)
    }

    @Test
    fun testPointToChildSpaceWithImagePoint() {
        val point = ImagePoint(150U, 150U)
        val expected = ImagePoint(100U, 100U)

        val result2 = point.toChildSpace(parentSpace, childSpace)
        assertEquals(expected, result2)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testPointToParentSpaceOutOfBoundsThrowsException() {
        pointToParentSpace(2000U, 2000U, childSpace, parentSpace)
    }
}
