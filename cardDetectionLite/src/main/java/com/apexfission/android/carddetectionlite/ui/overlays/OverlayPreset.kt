package com.apexfission.android.carddetectionlite.ui.overlays

import androidx.compose.runtime.Immutable

/**
 * Configuration preset for controlling UI overlays, indicators, and controls displayed over the camera feed.
 *
 * @property showBoundingBoxes When `true`, draws bounding boxes around detected objects.
 * @property showClassNames When `true` (and [showBoundingBoxes] is true), draws class labels and confidence scores above boxes.
 * @property showFlashlightSwitch When `true`, provides a UI button to toggle the camera torch.
 * @property showLockOnProgress When `true`, renders the animated lock-on progress frame overlay.
 * @property showDebugOverlay When `true`, displays a real-time panel with configuration state and performance metrics.
 * @property showFocusIndicator When `true`, displays a visual focus ring indicator when camera focus changes.
 * @property showAreaOfInterest When `true`, displays the area of interest (cropping region) with background dimming.
 */
@Immutable
data class OverlayPreset(
    val showBoundingBoxes: Boolean = false,
    val showClassNames: Boolean = false,
    val showFlashlightSwitch: Boolean = true,
    val showLockOnProgress: Boolean = true,
    val showDebugOverlay: Boolean = false,
    val showFocusIndicator: Boolean = true,
    val showAreaOfInterest: Boolean = false
) {
    /**
     * Returns a copy of this [OverlayPreset] with the specified properties modified.
     */
    fun change(
        showBoundingBoxes: Boolean = this.showBoundingBoxes,
        showClassNames: Boolean = this.showClassNames,
        showFlashlightSwitch: Boolean = this.showFlashlightSwitch,
        showLockOnProgress: Boolean = this.showLockOnProgress,
        showDebugOverlay: Boolean = this.showDebugOverlay,
        showFocusIndicator: Boolean = this.showFocusIndicator,
        showAreaOfInterest: Boolean = this.showAreaOfInterest
    ): OverlayPreset = copy(
        showBoundingBoxes = showBoundingBoxes,
        showClassNames = showClassNames,
        showFlashlightSwitch = showFlashlightSwitch,
        showLockOnProgress = showLockOnProgress,
        showDebugOverlay = showDebugOverlay,
        showFocusIndicator = showFocusIndicator,
        showAreaOfInterest = showAreaOfInterest
    )

    companion object {
        /**
         * Standard production view showing flashlight switch, lock-on progress overlay, and focus indicator.
         */
        val Standard = OverlayPreset(
            showBoundingBoxes = false,
            showClassNames = false,
            showFlashlightSwitch = true,
            showLockOnProgress = true,
            showDebugOverlay = false,
            showFocusIndicator = true,
            showAreaOfInterest = true
        )

        /**
         * Minimalist view rendering only the lock-on progress overlay.
         */
        val Minimal = OverlayPreset(
            showBoundingBoxes = false,
            showClassNames = false,
            showFlashlightSwitch = false,
            showLockOnProgress = true,
            showDebugOverlay = false,
            showFocusIndicator = false,
            showAreaOfInterest = false
        )

        /**
         * Comprehensive debug view displaying bounding boxes, class names, lock-on progress, flashlight switch, and debug panel.
         */
        val Debug = OverlayPreset(
            showBoundingBoxes = true,
            showClassNames = true,
            showFlashlightSwitch = true,
            showLockOnProgress = true,
            showDebugOverlay = true,
            showFocusIndicator = true,
            showAreaOfInterest = true
        )
    }
}
