package com.apexfission.android.permissionscompose

import org.junit.Assert.assertEquals
import org.junit.Test

class CameraPermissionStatusTest {

    @Test
    fun `granted takes precedence over request history`() {
        assertEquals(
            CameraPermissionStatus.Granted,
            resolveCameraPermissionStatus(
                granted = true,
                shouldShowRationale = false,
                requestedBefore = true,
            ),
        )
    }

    @Test
    fun `first request is distinguished from permanent denial`() {
        assertEquals(
            CameraPermissionStatus.NotRequested,
            resolveCameraPermissionStatus(
                granted = false,
                shouldShowRationale = false,
                requestedBefore = false,
            ),
        )
    }

    @Test
    fun `normal denial exposes rationale`() {
        assertEquals(
            CameraPermissionStatus.RationaleRequired,
            resolveCameraPermissionStatus(
                granted = false,
                shouldShowRationale = true,
                requestedBefore = true,
            ),
        )
    }

    @Test
    fun `denial without rationale after request is permanent`() {
        assertEquals(
            CameraPermissionStatus.PermanentlyDenied,
            resolveCameraPermissionStatus(
                granted = false,
                shouldShowRationale = false,
                requestedBefore = true,
            ),
        )
    }
}
