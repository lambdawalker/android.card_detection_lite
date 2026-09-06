package com.apexfission.android.carddetectionlite.domain.coordinates

import com.apexfission.android.carddetectionlite.domain.coordinates.models.ImageBox
import com.apexfission.android.carddetectionlite.domain.coordinates.models.ImagePoint
import com.apexfission.android.carddetectionlite.domain.coordinates.models.ImageSpace
import com.apexfission.android.carddetectionlite.domain.coordinates.models.NormImageBox
import com.apexfission.android.carddetectionlite.domain.coordinates.models.NormImagePoint
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
    fun testImageBoxFrom2P() {
        val box = ImageBox.from2P(x1 = 5U, y1 = 10U, x2 = 50U, y2 = 60U)
        assertEquals(5U, box.x)
        assertEquals(10U, box.y)
        assertEquals(50U, box.x2)
        assertEquals(60U, box.y2)
        assertEquals(45U, box.width)
        assertEquals(50U, box.height)
    }

    @Test
    fun testImageBoxFrom2PUnarranged() {
        val box = ImageBox.from2P(x1 = 50U, y1 = 60U, x2 = 5U, y2 = 10U)
        assertEquals(5U, box.x)
        assertEquals(10U, box.y)
        assertEquals(50U, box.x2)
        assertEquals(60U, box.y2)
        assertEquals(45U, box.width)
        assertEquals(50U, box.height)
    }

    @Test
    fun testImageBoxFromPSWithUInts() {
        val box = ImageBox.fromPS(x = 5U, y = 10U, width = 45U, height = 50U)
        assertEquals(5U, box.x)
        assertEquals(10U, box.y)
        assertEquals(50U, box.x2)
        assertEquals(60U, box.y2)
        assertEquals(45U, box.width)
        assertEquals(50U, box.height)
    }

    @Test
    fun testImageBoxFromPSWithInts() {
        val box = ImageBox.fromPS(x = 10, y = 20, width = 100, height = 150)
        assertEquals(10U, box.x)
        assertEquals(20U, box.y)
        assertEquals(110U, box.x2)
        assertEquals(170U, box.y2)
        assertEquals(100U, box.width)
        assertEquals(150U, box.height)
    }

    @Test
    fun testImageBoxFromPSWithNegativeInts() {
        val box = ImageBox.fromPS(x = -10, y = -20, width = -100, height = -150)
        assertEquals(0U, box.x)
        assertEquals(0U, box.y)
        assertEquals(0U, box.x2)
        assertEquals(0U, box.y2)
        assertEquals(0U, box.width)
        assertEquals(0U, box.height)
    }

    @Test
    fun testImageBoxFromPSWithMixedInts() {
        val box = ImageBox.fromPS(x = -5, y = 15, width = 30, height = -10)
        assertEquals(0U, box.x)
        assertEquals(15U, box.y)
        assertEquals(30U, box.x2)
        assertEquals(15U, box.y2)
        assertEquals(30U, box.width)
        assertEquals(0U, box.height)
    }

    @Test
    fun testNormImagePointProperties() {
        val normPoint = NormImagePoint(x = 0.25F, y = 0.75F)
        assertEquals(0.25F, normPoint.x, 0.0001F)
        assertEquals(0.75F, normPoint.y, 0.0001F)
    }

    @Test
    fun testNormImageBoxFrom2P() {
        val normBox = NormImageBox.from2P(x1 = 0.1F, y1 = 0.2F, x2 = 0.8F, y2 = 0.9F)
        assertEquals(0.1F, normBox.x, 0.0001F)
        assertEquals(0.2F, normBox.y, 0.0001F)
        assertEquals(0.8F, normBox.x2, 0.0001F)
        assertEquals(0.9F, normBox.y2, 0.0001F)
        assertEquals(0.7F, normBox.width, 0.0001F)
        assertEquals(0.7F, normBox.height, 0.0001F)
    }

    @Test
    fun testNormImageBoxFromPS() {
        val normBox = NormImageBox.fromPS(x = 0.1F, y = 0.2F, width = 0.7F, height = 0.7F)
        assertEquals(0.1F, normBox.x, 0.0001F)
        assertEquals(0.2F, normBox.y, 0.0001F)
        assertEquals(0.8F, normBox.x2, 0.0001F)
        assertEquals(0.9F, normBox.y2, 0.0001F)
        assertEquals(0.7F, normBox.width, 0.0001F)
        assertEquals(0.7F, normBox.height, 0.0001F)
    }
}
