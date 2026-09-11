package com.apexfission.android.carddetectionlite.ui

import com.apexfission.android.carddetectionlite.ui.detector.CardDetectorOverlayScopeImpl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CardDetectorOverlayScopeTest {

    @Test
    fun testOverlayScopeActionsAndState() {
        var captureCalled = false
        var backCalled = false
        var flashlightToggleCalled = false

        val scope = CardDetectorOverlayScopeImpl(
            detectionState = null,
            latestBestDetection = null,
            imageSpaceChain = null,
            flashlightAvailable = true,
            flashlightEnabled = false,
            onCaptureRequested = { captureCalled = true },
            onBackRequested = { backCalled = true },
            onFlashlightToggleRequested = { flashlightToggleCalled = true }
        )

        assertNull(scope.detectionState)
        assertNull(scope.cardDetection)
        assertNull(scope.latestBestDetection)
        assertNull(scope.imageSpaceChain)
        assertFalse(scope.captureEnabled)
        assertTrue(scope.flashlightAvailable)
        assertFalse(scope.flashlightEnabled)

        scope.capture()
        assertTrue(captureCalled)

        scope.goBack()
        assertTrue(backCalled)

        scope.toggleFlashlight()
        assertTrue(flashlightToggleCalled)
    }
}
