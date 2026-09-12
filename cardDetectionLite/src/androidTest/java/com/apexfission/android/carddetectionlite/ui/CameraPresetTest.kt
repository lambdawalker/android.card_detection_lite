package com.apexfission.android.carddetectionlite.ui

import android.util.Size
import androidx.annotation.WorkerThread
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.apexfission.android.carddetectionlite.ui.camerapreview.CameraPreset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CameraPresetTest {

    @Test
    fun testDefaultCameraPreset() {
        val preset = CameraPreset.Default

        assertEquals(Size(2048, 1080), preset.analysisTargetResolution)
        assertTrue(preset.tapToFocusEnabled)
        assertTrue(preset.focusOnCardEnabled)
    }

    @Test
    fun testHighResolutionCameraPreset() {
        val preset = CameraPreset.HighResolution

        assertEquals(Size(3840, 2160), preset.analysisTargetResolution)
        assertTrue(preset.tapToFocusEnabled)
        assertTrue(preset.focusOnCardEnabled)
    }

    @Test
    fun testFixedFocusCameraPreset() {
        val preset = CameraPreset.FixedFocus

        assertEquals(Size(1920, 1080), preset.analysisTargetResolution)
        assertFalse(preset.tapToFocusEnabled)
        assertFalse(preset.focusOnCardEnabled)
    }

    @Test
    fun testCameraPresetCopy() {
        val original = CameraPreset.Default
        val modified = original.copy(focusOnCardEnabled = false)

        assertEquals(original.analysisTargetResolution, modified.analysisTargetResolution)
        assertTrue(modified.tapToFocusEnabled)
        assertFalse(modified.focusOnCardEnabled)
    }
}
