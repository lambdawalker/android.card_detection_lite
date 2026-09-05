package com.apexfission.android.carddetectionlite.domain.coordinates

import org.junit.Assert.assertEquals
import org.junit.Test

class ModelsTest {

    @Test
    fun testImageSpaceDefaultsAndProperties() {
        val space = ImageSpace(width = 100U, height = 200U)
        assertEquals(100U, space.width)
        assertEquals(200U, space.height)
        assertEquals(1.0F, space.xScale, 0.0001F)
        assertEquals(1.0F, space.yScale, 0.0001F)
        assertEquals(0U, space.xOffset)
        assertEquals(0U, space.yOffset)
    }

    @Test
    fun testImagePointFactoryAndProperties() {
        val point1 = ImagePoint(10U, 20U)
        assertEquals(10U, point1.x)
        assertEquals(20U, point1.y)

        val point2 = ImagePoint(10, 20)
        assertEquals(point1, point2)
    }

    @Test
    fun testImageBox2PProperties() {
        val box = ImageBox2P(x = 5U, y = 10U, x2 = 50U, y2 = 60U)
        assertEquals(5U, box.x)
        assertEquals(10U, box.y)
        assertEquals(50U, box.x2)
        assertEquals(60U, box.y2)
    }

    @Test
    fun testImageBoxPSProperties() {
        val box = ImageBoxPS(x = 5U, y = 10U, width = 45, height = 50)
        assertEquals(5U, box.x)
        assertEquals(10U, box.y)
        assertEquals(45, box.width)
        assertEquals(50, box.height)
    }

    @Test
    fun testNormImagePointProperties() {
        val normPoint = NormImagePoint(x = 0.25F, y = 0.75F)
        assertEquals(0.25F, normPoint.x, 0.0001F)
        assertEquals(0.75F, normPoint.y, 0.0001F)
    }

    @Test
    fun testNormImageBox2PProperties() {
        val normBox = NormImageBox2P(x = 0.1F, y = 0.2F, x2 = 0.8F, y2 = 0.9F)
        assertEquals(0.1F, normBox.x, 0.0001F)
        assertEquals(0.2F, normBox.y, 0.0001F)
        assertEquals(0.8F, normBox.x2, 0.0001F)
        assertEquals(0.9F, normBox.y2, 0.0001F)
    }

    @Test
    fun testNormImageBoxPSProperties() {
        val normBox = NormImageBoxPS(x = 0.1F, y = 0.2F, width = 0.7F, height = 0.7F)
        assertEquals(0.1F, normBox.x, 0.0001F)
        assertEquals(0.2F, normBox.y, 0.0001F)
        assertEquals(0.7F, normBox.width, 0.0001F)
        assertEquals(0.7F, normBox.height, 0.0001F)
    }
}
