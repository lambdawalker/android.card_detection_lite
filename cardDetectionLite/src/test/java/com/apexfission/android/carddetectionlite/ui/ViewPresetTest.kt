package com.apexfission.android.carddetectionlite.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ViewPresetTest {

    @Test
    fun testStandardViewPreset() {
        val preset = ViewPreset.Standard

        assertFalse(preset.showBoundingBoxes)
        assertFalse(preset.showClassNames)
        assertTrue(preset.showFlashlightSwitch)
        assertTrue(preset.showLockOnProgress)
        assertFalse(preset.showDebugOverlay)
        assertTrue(preset.showFocusIndicator)
    }

    @Test
    fun testMinimalViewPreset() {
        val preset = ViewPreset.Minimal

        assertFalse(preset.showBoundingBoxes)
        assertFalse(preset.showClassNames)
        assertFalse(preset.showFlashlightSwitch)
        assertTrue(preset.showLockOnProgress)
        assertFalse(preset.showDebugOverlay)
        assertFalse(preset.showFocusIndicator)
    }

    @Test
    fun testDebugViewPreset() {
        val preset = ViewPreset.Debug

        assertTrue(preset.showBoundingBoxes)
        assertTrue(preset.showClassNames)
        assertTrue(preset.showFlashlightSwitch)
        assertTrue(preset.showLockOnProgress)
        assertTrue(preset.showDebugOverlay)
        assertTrue(preset.showFocusIndicator)
    }

    @Test
    fun testViewPresetCustomCopy() {
        val original = ViewPreset.Standard
        val modified = original.copy(showBoundingBoxes = true, showDebugOverlay = true)

        assertTrue(modified.showBoundingBoxes)
        assertTrue(modified.showDebugOverlay)
        assertTrue(modified.showLockOnProgress)
    }
}
