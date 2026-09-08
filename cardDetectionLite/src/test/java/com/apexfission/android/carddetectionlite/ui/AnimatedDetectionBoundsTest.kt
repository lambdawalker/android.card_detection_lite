package com.apexfission.android.carddetectionlite.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import com.apexfission.android.carddetectionlite.ui.overlays.AnimatedDetectionBounds
import com.apexfission.android.carddetectionlite.ui.overlays.DetectionAnimationConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AnimatedDetectionBoundsTest {

    @Test
    fun testAnimatedDetectionBoundsProperties() {
        val bounds = AnimatedDetectionBounds(
            left = 100f,
            top = 200f,
            right = 300f,
            bottom = 500f,
            lockOnProgress = 0.8f,
            opacity = 0.85f,
            isTracking = true
        )

        assertEquals(200f, bounds.width, 0.001f)
        assertEquals(300f, bounds.height, 0.001f)
        assertEquals(Offset(200f, 350f), bounds.center)
        assertEquals(Offset(100f, 200f), bounds.topLeft)
        assertEquals(Size(200f, 300f), bounds.size)
        assertEquals(0.8f, bounds.lockOnProgress, 0.001f)
        assertEquals(0.85f, bounds.opacity, 0.001f)
        assertTrue(bounds.isTracking)
        assertNull(bounds.activeDetection)
    }

    @Test
    fun testDetectionAnimationConfigDefaults() {
        val config = DetectionAnimationConfig()

        assertEquals(0.35f, config.idleOpacity, 0.001f)
        assertEquals(0.85f, config.detectedOpacity, 0.001f)
        assertEquals(2_000L, config.missingCardResetDelayMs)
        assertNull(config.maxConsecutiveMisses)
        assertEquals(200, config.trackingAnimationDurationMs)
        assertEquals(300, config.resetAnimationDurationMs)
        assertEquals(1_000, config.fadeAnimationDurationMs)
        assertTrue(config.enableGuideSmoothing)
    }
}
