package com.apexfission.android.carddetectionlite.ui.overlays.animation.x

import com.apexfission.android.carddetectionlite.domain.coordinates.models.ImageBox

internal data class GuideStateResult(
    val targetBox: ImageBox?,
    val targetOpacity: Float,
    val opacityDurationMs: Int,
    val boundsDurationMs: Int
)
