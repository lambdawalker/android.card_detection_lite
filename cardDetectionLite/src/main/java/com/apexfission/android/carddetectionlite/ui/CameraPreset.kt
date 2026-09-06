package com.apexfission.android.carddetectionlite.ui

import android.util.Size
import androidx.compose.runtime.Immutable

/**
 * Configuration preset for tuning CameraX analysis resolution, tap-to-focus, and auto-focus behavior.
 *
 * @property analysisTargetResolution Target resolution size for the camera image analysis stream (defaults to 2048x1080).
 * @property tapToFocusEnabled Enables or disables user tap-to-focus touch gestures.
 * @property focusOnCardEnabled Enables or disables smart auto-focus tracking on detected card positions.
 */
@Immutable
data class CameraPreset(
    val analysisTargetResolution: Size = Size(2048, 1080),
    val tapToFocusEnabled: Boolean = true,
    val focusOnCardEnabled: Boolean = true
) {
    companion object {
        /**
         * Default camera preset configured with 2K analysis resolution and full focus capabilities enabled.
         */
        val Default = CameraPreset(
            analysisTargetResolution = Size(2048, 1080),
            tapToFocusEnabled = true,
            focusOnCardEnabled = true
        )

        /**
         * High-resolution camera preset for devices with high-density sensors (4K target resolution).
         */
        val HighResolution = CameraPreset(
            analysisTargetResolution = Size(3840, 2160),
            tapToFocusEnabled = true,
            focusOnCardEnabled = true
        )

        /**
         * Fixed focus camera preset disabling tap-to-focus and smart auto-focus.
         */
        val FixedFocus = CameraPreset(
            analysisTargetResolution = Size(1920, 1080),
            tapToFocusEnabled = false,
            focusOnCardEnabled = false
        )
    }
}
