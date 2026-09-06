package com.apexfission.android.carddetectionlite.domain.coordinates.operations

import com.apexfission.android.carddetectionlite.domain.coordinates.models.ImageBox
import com.apexfission.android.carddetectionlite.domain.coordinates.models.ImageSpace
import com.apexfission.android.carddetectionlite.domain.coordinates.models.NormImageBox
import kotlin.math.round

/**
 * Converts a normalized bounding box [NormImageBox] to an absolute pixel-based [ImageBox]
 * using the dimensions of the given [localSpace].
 *
 * @param box The normalized bounding box to convert.
 * @param localSpace The coordinate space whose width and height define the scaling factors.
 * @return The converted [ImageBox] in absolute pixel coordinates.
 */
fun normBoxToBox(box: NormImageBox, localSpace: ImageSpace): ImageBox {
    return ImageBox.from2P(
        x1 = round(box.x * localSpace.width.toDouble()).toLong().coerceAtLeast(0).toUInt(),
        y1 = round(box.y * localSpace.height.toDouble()).toLong().coerceAtLeast(0).toUInt(),
        x2 = round(box.x2 * localSpace.width.toDouble()).toLong().coerceAtLeast(0).toUInt(),
        y2 = round(box.y2 * localSpace.height.toDouble()).toLong().coerceAtLeast(0).toUInt()
    )
}

/**
 * Extension function to convert this [NormImageBox] to an absolute pixel-based [ImageBox]
 * using the dimensions of the given [localSpace].
 *
 * @param localSpace The coordinate space whose width and height define the scaling factors.
 * @return The converted [ImageBox] in absolute pixel coordinates.
 */
fun NormImageBox.toBox(localSpace: ImageSpace): ImageBox = normBoxToBox(this, localSpace)
