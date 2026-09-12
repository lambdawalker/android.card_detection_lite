package com.apexfission.android.carddetectionlite.ui

import com.apexfission.android.carddetectionlite.ui.overlays.shouldRunContinuousAnimations
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ContinuousAnimationPolicyTest {

    @Test
    fun `runs while tracking visible pixels and motion is enabled`() {
        assertTrue(
            shouldRunContinuousAnimations(
                isTracking = true,
                opacity = 0.85f,
                motionEnabled = true,
                hasVisibleBounds = true,
            )
        )
    }

    @Test
    fun `stops while guide is not tracking`() {
        assertFalse(
            shouldRunContinuousAnimations(
                isTracking = false,
                opacity = 0.85f,
                motionEnabled = true,
                hasVisibleBounds = true,
            )
        )
    }

    @Test
    fun `stops when guide is fully transparent`() {
        assertFalse(
            shouldRunContinuousAnimations(
                isTracking = true,
                opacity = 0f,
                motionEnabled = true,
                hasVisibleBounds = true,
            )
        )
    }

    @Test
    fun `stops when continuous motion is disabled`() {
        assertFalse(
            shouldRunContinuousAnimations(
                isTracking = true,
                opacity = 1f,
                motionEnabled = false,
                hasVisibleBounds = true,
            )
        )
    }

    @Test
    fun `does not run for non-finite opacity`() {
        assertFalse(shouldRunContinuousAnimations(true, Float.NaN, true, true))
        assertFalse(shouldRunContinuousAnimations(true, Float.POSITIVE_INFINITY, true, true))
    }

    @Test
    fun `stops when bounds have no drawable area`() {
        assertFalse(shouldRunContinuousAnimations(true, 1f, true, false))
    }
}
