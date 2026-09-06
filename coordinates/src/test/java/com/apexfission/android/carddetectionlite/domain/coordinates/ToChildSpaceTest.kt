package com.apexfission.android.carddetectionlite.domain.coordinates

import com.apexfission.android.carddetectionlite.domain.coordinates.models.ImagePoint
import com.apexfission.android.carddetectionlite.domain.coordinates.models.ImageSpace
import com.apexfission.android.carddetectionlite.domain.coordinates.transformations.toChildSpace
import org.junit.Test


class ToChildSpaceTest {
    @Test
    fun sameSpace() {
        val parentSpace = ImageSpace(
            width = 500U, height = 500U,
        )

        val childSpace = ImageSpace(
            width = 500U, height = 500U
        )

        val imagePoint = ImagePoint(10, 10)
        val result = imagePoint.toChildSpace(parentSpace, childSpace)

        assert(imagePoint == result)
    }

    @Test
    fun croppedSpace() {
        val parentSpace = ImageSpace(
            width = 500U, height = 500U,
        )

        val childSpace = ImageSpace(
            width = 200U, height = 200U
        )

        val imagePoint = ImagePoint(10, 10)
        val result = imagePoint.toChildSpace(parentSpace, childSpace)

        assert(imagePoint == result)
    }

    @Test
    fun croppedAndMovedSpace() {
        val parentSpace = ImageSpace(
            width = 500U, height = 500U,
        )

        val childSpace = ImageSpace(
            width = 200U, height = 200U,
            xOffset = 100U, yOffset = 100U
        )

        val imagePoint = ImagePoint(150, 150)
        val expected = ImagePoint(50, 50)
        val result = imagePoint.toChildSpace(parentSpace, childSpace)

        assert(expected == result)
    }


    @Test
    fun croppedMovedAndScaledSpace() {
        val parentSpace = ImageSpace(
            width = 500U, height = 500U,
        )

        val childSpace = ImageSpace(
            width = 400U, height = 400U,
            xOffset = 100U, yOffset = 100U,
            xScale = 2F, yScale = 2F
        )

        val imagePoint = ImagePoint(150, 150)
        val expected = ImagePoint(100, 100)
        val result = imagePoint.toChildSpace(parentSpace, childSpace)

        assert(expected == result)
    }
}