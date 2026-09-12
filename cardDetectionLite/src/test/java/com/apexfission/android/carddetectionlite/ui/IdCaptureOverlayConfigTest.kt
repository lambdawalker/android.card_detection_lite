package com.apexfission.android.carddetectionlite.ui

import androidx.compose.ui.graphics.Color
import com.apexfission.android.carddetectionlite.ui.overlays.IdCaptureOverlayConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class IdCaptureOverlayConfigTest {

    @Test
    fun testDefaultConfigValues() {
        val config = IdCaptureOverlayConfig()

        assertEquals(0.35f, config.idleOpacity, 0.001f)
        assertEquals(0.85f, config.detectedOpacity, 0.001f)
        assertEquals(2_000L, config.missingCardResetDelayMs)
        assertNull(config.maxConsecutiveMisses)
        assertEquals(200, config.trackingAnimationDurationMs)
        assertEquals(300, config.resetAnimationDurationMs)
        assertEquals(1_000, config.fadeAnimationDurationMs)
        assertTrue(config.enableGuideSmoothing)
        assertFalse(config.enableContinuousAnimations)
        assertEquals(0.2f, config.guideSmoothingFactor, 0.001f)
        assertTrue(config.requiresCardDetectionForCapture)
        assertEquals("Verify Your Identity", config.title)
        assertEquals("Position your ID within the frame", config.instructionTitle)
        assertEquals("We'll use this to pre-fill your information securely", config.instructionSubTitle)
        assertEquals(Color(0xFF2979FF), config.guideColor)
    }

    @Test
    fun testCustomConfigCopy() {
        val original = IdCaptureOverlayConfig()
        val custom = original.copy(
            idleOpacity = 0.4f,
            detectedOpacity = 0.9f,
            missingCardResetDelayMs = 3_000L,
            maxConsecutiveMisses = 10,
            requiresCardDetectionForCapture = false
        )

        assertEquals(0.4f, custom.idleOpacity, 0.001f)
        assertEquals(0.9f, custom.detectedOpacity, 0.001f)
        assertEquals(3_000L, custom.missingCardResetDelayMs)
        assertEquals(Integer.valueOf(10), custom.maxConsecutiveMisses)
        assertEquals(false, custom.requiresCardDetectionForCapture)
    }
}
