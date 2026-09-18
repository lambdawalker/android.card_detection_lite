package com.apexfission.android.carddetectionlite.ui.camerapreview

import com.apexfission.android.math.models.ImageSpace
import com.apexfission.android.math.models.ImageSpaceChain
import com.apexfission.android.math.models.SpaceRelationship
import com.apexfission.android.math.models.chain
import com.apexfission.android.math.models.cropAtCenter
import com.apexfission.android.math.models.scale

/**
 * Creates an [ImageSpaceChain] mapping coordinates from source frame dimensions to preview display dimensions.
 *
 * @param videoWidth Source video width in pixels.
 * @param videoHeight Source video height in pixels.
 * @param viewWidth Viewport display width in pixels.
 * @param viewHeight Viewport display height in pixels.
 * @return An [ImageSpaceChain] containing source, scaled, and cropped space definitions.
 */
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

    return source.chain(scaled, SpaceRelationship.Child).chain(cropped, SpaceRelationship.Child)
}
