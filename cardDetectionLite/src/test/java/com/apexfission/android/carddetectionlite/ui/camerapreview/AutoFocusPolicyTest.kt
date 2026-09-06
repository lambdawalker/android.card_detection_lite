package com.apexfission.android.carddetectionlite.ui.camerapreview

import android.graphics.Bitmap
import com.apexfission.android.carddetectionlite.domain.coordinates.models.ImageBox
import com.apexfission.android.carddetectionlite.domain.tflite.model.CardDetection
import com.apexfission.android.carddetectionlite.domain.tflite.model.Feature
import com.apexfission.android.carddetectionlite.domain.tflite.model.LockingStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.mock

class AutoFocusPolicyTest {

    private val dummyBitmap: Bitmap = mock(Bitmap::class.java)

    private fun createDetection(x1: UInt, y1: UInt, x2: UInt, y2: UInt): CardDetection {
        val box = ImageBox.from2P(x1, y1, x2, y2)
        val feature = Feature(box = box, confidence = 0.9f, classId = 0, image = dummyBitmap)
        return CardDetection(
            lockOnProgress = 1.0f,
            id = 1L,
            card = feature,
            features = emptyList(),
            lockingStatus = LockingStatus.CardLocked
        )
    }

    @Test
    fun testNullDetectionReturnsShouldFocusFalse() {
        val policy = AutoFocusPolicy()
        val result = policy.shouldTriggerFocus(null)

        assertFalse(result.shouldFocus)
        assertNull(result.focusPoint)
    }

    @Test
    fun testInitialDetectionTriggersFocus() {
        val policy = AutoFocusPolicy(cooldownMs = 500L)
        val detection = createDetection(100U, 100U, 300U, 300U)

        val result = policy.shouldTriggerFocus(detection, currentTimeMs = 1000L)

        assertTrue(result.shouldFocus)
        assertNotNull(result.focusPoint)
        assertEquals(200f, result.focusPoint?.x ?: 0f, 0.001f)
        assertEquals(200f, result.focusPoint?.y ?: 0f, 0.001f)
    }

    @Test
    fun testCooldownPreventsFocusWithinCooldownPeriod() {
        val policy = AutoFocusPolicy(cooldownMs = 500L, positionThresholdPx = 10f)

        val detection1 = createDetection(100U, 100U, 300U, 300U)
        val result1 = policy.shouldTriggerFocus(detection1, currentTimeMs = 1000L)
        assertTrue(result1.shouldFocus)

        val detection2 = createDetection(500U, 500U, 700U, 700U)
        val result2 = policy.shouldTriggerFocus(detection2, currentTimeMs = 1200L)

        assertFalse("Focus should be blocked during cooldown", result2.shouldFocus)
    }

    @Test
    fun testFocusTriggersAfterCooldownWhenMovedSignificantly() {
        val policy = AutoFocusPolicy(cooldownMs = 500L, positionThresholdPx = 50f)

        val detection1 = createDetection(100U, 100U, 300U, 300U)
        policy.shouldTriggerFocus(detection1, currentTimeMs = 1000L)

        val detection2 = createDetection(200U, 200U, 400U, 400U)
        val result2 = policy.shouldTriggerFocus(detection2, currentTimeMs = 1600L)

        assertTrue(result2.shouldFocus)
        assertEquals(300f, result2.focusPoint?.x ?: 0f, 0.001f)
        assertEquals(300f, result2.focusPoint?.y ?: 0f, 0.001f)
    }

    @Test
    fun testFocusTriggersAfterCooldownWhenAreaChangesSignificantly() {
        val policy = AutoFocusPolicy(cooldownMs = 500L, areaChangeThreshold = 0.10f)

        val detection1 = createDetection(100U, 100U, 300U, 300U)
        policy.shouldTriggerFocus(detection1, currentTimeMs = 1000L)

        val detection2 = createDetection(50U, 50U, 350U, 350U)
        val result2 = policy.shouldTriggerFocus(detection2, currentTimeMs = 1600L)

        assertTrue(result2.shouldFocus)
    }

    @Test
    fun testNoFocusWhenStationaryAndCooldownPassed() {
        val policy = AutoFocusPolicy(cooldownMs = 500L, positionThresholdPx = 50f, areaChangeThreshold = 0.10f)

        val detection = createDetection(100U, 100U, 300U, 300U)
        policy.shouldTriggerFocus(detection, currentTimeMs = 1000L)

        val result2 = policy.shouldTriggerFocus(detection, currentTimeMs = 2000L)

        assertFalse(result2.shouldFocus)
    }

    @Test
    fun testResetClearsTrackingState() {
        val policy = AutoFocusPolicy(cooldownMs = 500L)

        val detection = createDetection(100U, 100U, 300U, 300U)
        policy.shouldTriggerFocus(detection, currentTimeMs = 1000L)

        policy.reset()

        val result = policy.shouldTriggerFocus(detection, currentTimeMs = 1000L)
        assertTrue(result.shouldFocus)
    }
}
