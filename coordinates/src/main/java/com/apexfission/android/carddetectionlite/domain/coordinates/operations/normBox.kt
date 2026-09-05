package com.apexfission.android.carddetectionlite.domain.coordinates.operations

import com.apexfission.android.carddetectionlite.domain.coordinates.ImageBox2P
import com.apexfission.android.carddetectionlite.domain.coordinates.ImageBoxPS
import com.apexfission.android.carddetectionlite.domain.coordinates.ImageSpace
import com.apexfission.android.carddetectionlite.domain.coordinates.NormImageBox2P
import com.apexfission.android.carddetectionlite.domain.coordinates.NormImageBoxPS

/**
 * Converts a normalized bounding box [NormImageBox2P] to an absolute pixel-based [ImageBox2P]
 * using the dimensions of the given [localSpace].
 *
 * @param box The normalized bounding box to convert.
 * @param localSpace The coordinate space whose width and height define the scaling factors.
 * @return The converted [ImageBox2P] in absolute pixel coordinates.
 */
fun normBox2PToBox2P(box: NormImageBox2P, localSpace: ImageSpace): ImageBox2P {
    return ImageBox2P(
        x = (box.x * localSpace.width.toDouble()).toUInt(),
        y = (box.y * localSpace.height.toDouble()).toUInt(),
        x2 = (box.x2 * localSpace.width.toDouble()).toUInt(),
        y2 = (box.y2 * localSpace.height.toDouble()).toUInt()
    )
}

/**
 * Extension function to convert this [NormImageBox2P] to an absolute pixel-based [ImageBox2P]
 * using the dimensions of the given [localSpace].
 *
 * @param localSpace The coordinate space whose width and height define the scaling factors.
 * @return The converted [ImageBox2P] in absolute pixel coordinates.
 */
fun NormImageBox2P.toBox2P(localSpace: ImageSpace): ImageBox2P = normBox2PToBox2P(this, localSpace)

/**
 * Converts a normalized bounding box [NormImageBoxPS] (point + size) to an absolute pixel-based [ImageBoxPS]
 * using the dimensions of the given [localSpace].
 *
 * @param box The normalized point-size bounding box to convert.
 * @param localSpace The coordinate space whose width and height define the scaling factors.
 * @return The converted [ImageBoxPS] in absolute pixel coordinates.
 */
fun normBoxPStoBoxPS(box: NormImageBoxPS, localSpace: ImageSpace): ImageBoxPS {
    return ImageBoxPS(
        x = (box.x * localSpace.width.toDouble()).toUInt(),
        y = (box.y * localSpace.height.toDouble()).toUInt(),
        width = (box.width * localSpace.width.toDouble()).toInt(),
        height = (box.height * localSpace.height.toDouble()).toInt()
    )
}

/**
 * Extension function to convert this [NormImageBoxPS] to an absolute pixel-based [ImageBoxPS]
 * using the dimensions of the given [localSpace].
 *
 * @param localSpace The coordinate space whose width and height define the scaling factors.
 * @return The converted [ImageBoxPS] in absolute pixel coordinates.
 */
fun NormImageBoxPS.toBoxPS(localSpace: ImageSpace): ImageBoxPS = normBoxPStoBoxPS(this, localSpace)
