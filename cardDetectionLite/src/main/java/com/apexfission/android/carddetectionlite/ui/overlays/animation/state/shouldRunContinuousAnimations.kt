package com.apexfission.android.carddetectionlite.ui.overlays.animation.state

internal fun shouldRunContinuousAnimations(
    isTracking: Boolean,
    opacity: Float,
    motionEnabled: Boolean,
    hasVisibleBounds: Boolean,
): Boolean = isTracking && opacity.isFinite() && opacity > 0f && motionEnabled && hasVisibleBounds
