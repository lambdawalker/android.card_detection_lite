package com.apexfission.android.carddetectionlite.ui

import com.apexfission.android.carddetectionlite.domain.coordinates.models.ImageSpace
import com.apexfission.android.carddetectionlite.domain.coordinates.models.ImageSpaceChain
import com.apexfission.android.carddetectionlite.domain.coordinates.models.cropAtCenter
import com.apexfission.android.carddetectionlite.domain.coordinates.models.scale

fun createPreviewImageSpaceChain(
    videoWidth: Int,
    videoHeight: Int,
    viewWidth: Int,
    viewHeight: Int
): ImageSpaceChain {
    val scale = maxOf(
        viewWidth / videoWidth.toFloat(),
        viewHeight / videoHeight.toFloat()
    )

    val source = ImageSpace(
        width = videoWidth.toUInt(),
        height = videoHeight.toUInt()
    )

    val scaled = source.scale(scale)

    val cropped = scaled.cropAtCenter(
        viewWidth.toUInt(),
        viewHeight.toUInt()
    )

    return listOf(source, scaled, cropped)
}

