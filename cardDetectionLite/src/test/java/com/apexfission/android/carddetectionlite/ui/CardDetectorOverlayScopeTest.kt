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

        val testLabels = mapOf(0 to "horizontal_card")
        val scope = CardDetectorOverlayScopeImpl(
            detectionState = null,
            latestValidDetection = null,
            imageSpaceChain = null,
            flashlightAvailable = true,
            flashlightEnabled = false,
            classLabels = testLabels,
            onCaptureRequested = { captureCalled = true },
            onBackRequested = { backCalled = true },
            onFlashlightToggleRequested = { flashlightToggleCalled = true }
        )

        assertNull(scope.detectionState)
        assertNull(scope.cardDetection)
        assertNull(scope.latestValidDetection)
        assertNull(scope.imageSpaceChain)
        assertFalse(scope.captureEnabled)
        assertTrue(scope.flashlightAvailable)
        assertFalse(scope.flashlightEnabled)
        assertEquals(testLabels, scope.classLabels)

        scope.capture()
        assertTrue(captureCalled)

        scope.goBack()
        assertTrue(backCalled)

        scope.toggleFlashlight()
        assertTrue(flashlightToggleCalled)
    }
}
