package com.apexfission.android.carddetectionlite.ui.camerapreview

import com.apexfission.android.carddetectionlite.domain.coordinates.models.ImageSpaceChain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FocusPointTransformTest {
    @Test
    fun `source center maps to preview center after fill-center crop`() {
        val chain = createPreviewImageSpaceChain(
            videoWidth = 640,
            videoHeight = 480,
            viewWidth = 1080,
            viewHeight = 1920,
        )

        val previewPoint = FocusPoint(320f, 240f).toPreviewSpace(chain)

        assertEquals(540f, previewPoint?.x ?: Float.NaN, 0.001f)
        assertEquals(960f, previewPoint?.y ?: Float.NaN, 0.001f)
    }

    @Test
    fun `point accounts for horizontal pixels removed by center crop`() {
        val chain = createPreviewImageSpaceChain(
            videoWidth = 640,
            videoHeight = 480,
            viewWidth = 1080,
            viewHeight = 1920,
        )

        val previewPoint = FocusPoint(240f, 240f).toPreviewSpace(chain)

        assertEquals(220f, previewPoint?.x ?: Float.NaN, 0.001f)
        assertEquals(960f, previewPoint?.y ?: Float.NaN, 0.001f)
    }

    @Test
    fun `focus is skipped until a coordinate chain is available`() {
        assertNull(FocusPoint(320f, 240f).toPreviewSpace(ImageSpaceChain.Empty))
    }
}
