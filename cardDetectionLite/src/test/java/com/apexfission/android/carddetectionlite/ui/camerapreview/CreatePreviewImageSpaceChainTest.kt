package com.apexfission.android.carddetectionlite.ui.camerapreview

import com.apexfission.android.carddetectionlite.ui.overlays.createPreviewImageSpaceChain
import org.junit.Assert.assertEquals
import org.junit.Test

class CreatePreviewImageSpaceChainTest {

    @Test
    fun testCreatePreviewImageSpaceChainLandscapeVideoInPortraitView() {
        val videoWidth = 1920
        val videoHeight = 1080
        val viewWidth = 1080
        val viewHeight = 1920

        val chain = createPreviewImageSpaceChain(
            videoWidth = videoWidth,
            videoHeight = videoHeight,
            viewWidth = viewWidth,
            viewHeight = viewHeight
        )

        assertEquals(3, chain.size)

        val sourceSpace = chain[0]
        assertEquals(1920U, sourceSpace.width)
        assertEquals(1080U, sourceSpace.height)

        val scaledSpace = chain[1]
        val expectedScale = maxOf(1080f / 1920f, 1920f / 1080f)
        assertEquals(expectedScale, scaledSpace.xScale, 0.001f)
        assertEquals(expectedScale, scaledSpace.yScale, 0.001f)

        val croppedSpace = chain[2]
        assertEquals(1080U, croppedSpace.width)
        assertEquals(1920U, croppedSpace.height)
    }

    @Test
    fun testCreatePreviewImageSpaceChainMatchingAspectRatios() {
        val videoWidth = 1080
        val videoHeight = 1920
        val viewWidth = 1080
        val viewHeight = 1920

        val chain = createPreviewImageSpaceChain(
            videoWidth = videoWidth,
            videoHeight = videoHeight,
            viewWidth = viewWidth,
            viewHeight = viewHeight
        )

        assertEquals(3, chain.size)

        val sourceSpace = chain[0]
        val scaledSpace = chain[1]
        val croppedSpace = chain[2]

        assertEquals(1080U, sourceSpace.width)
        assertEquals(1920U, sourceSpace.height)

        assertEquals(1.0f, scaledSpace.xScale, 0.0001f)
        assertEquals(1.0f, scaledSpace.yScale, 0.0001f)

        assertEquals(1080U, croppedSpace.width)
        assertEquals(1920U, croppedSpace.height)
        assertEquals(0U, croppedSpace.xOffset)
        assertEquals(0U, croppedSpace.yOffset)
    }

    @Test
    fun testCreatePreviewImageSpaceChainPortraitVideoInLandscapeView() {
        val videoWidth = 1080
        val videoHeight = 1920
        val viewWidth = 1920
        val viewHeight = 1080

        val chain = createPreviewImageSpaceChain(
            videoWidth = videoWidth,
            videoHeight = videoHeight,
            viewWidth = viewWidth,
            viewHeight = viewHeight
        )

        assertEquals(3, chain.size)

        val sourceSpace = chain[0]
        val croppedSpace = chain[2]

        assertEquals(1080U, sourceSpace.width)
        assertEquals(1920U, sourceSpace.height)

        assertEquals(1920U, croppedSpace.width)
        assertEquals(1080U, croppedSpace.height)
    }
}
