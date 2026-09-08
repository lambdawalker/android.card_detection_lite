package com.apexfission.android.carddetectionlite.ui

import com.apexfission.android.carddetectionlite.ui.camerapreview.CameraPreset
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CameraPresetTest {

    @Test
    fun testDefaultCameraPreset() {
        val preset = CameraPreset.Default

        assertNotNull(preset.analysisTargetResolution)
        assertTrue(preset.tapToFocusEnabled)
        assertTrue(preset.focusOnCardEnabled)
    }

    @Test
    fun testHighResolutionCameraPreset() {
        val preset = CameraPreset.HighResolution

        assertNotNull(preset.analysisTargetResolution)
        assertTrue(preset.tapToFocusEnabled)
        assertTrue(preset.focusOnCardEnabled)
    }

    @Test
    fun testFixedFocusCameraPreset() {
        val preset = CameraPreset.FixedFocus

        assertNotNull(preset.analysisTargetResolution)
        assertFalse(preset.tapToFocusEnabled)
        assertFalse(preset.focusOnCardEnabled)
    }

    @Test
    fun testCameraPresetCopy() {
        val original = CameraPreset.Default
        val modified = original.copy(focusOnCardEnabled = false)

        assertNotNull(modified.analysisTargetResolution)
        assertTrue(modified.tapToFocusEnabled)
        assertFalse(modified.focusOnCardEnabled)
    }

    @Test
    fun testCameraPresetChange() {
        val original = CameraPreset.Default
        val modified = original.change(focusOnCardEnabled = false)

        assertNotNull(modified.analysisTargetResolution)
        assertTrue(modified.tapToFocusEnabled)
        assertFalse(modified.focusOnCardEnabled)
    }
}


