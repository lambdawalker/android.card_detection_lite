package com.apexfission.android.carddetectionlite.domain.coordinates.operations

import com.apexfission.android.carddetectionlite.domain.coordinates.ImagePoint
import com.apexfission.android.carddetectionlite.domain.coordinates.ImageSpace
import com.apexfission.android.carddetectionlite.domain.coordinates.NormImagePoint

/**
 * Converts a normalized point [NormImagePoint] to an absolute pixel-based [ImagePoint]
 * using the dimensions of the given [localSpace].
 *
 * @param normPoint The normalized point to convert.
 * @param localSpace The coordinate space whose width and height define the scaling factors.
 * @return The converted [ImagePoint] in absolute pixel coordinates.
 */
fun normPointToPoint(normPoint: NormImagePoint, localSpace: ImageSpace): ImagePoint {
    return ImagePoint(
        x = (normPoint.x * localSpace.width.toFloat()).toUInt(),
        y = (normPoint.y * localSpace.height.toFloat()).toUInt()
    )
}

/**
 * Extension function to convert this [NormImagePoint] to an absolute pixel-based [ImagePoint]
 * using the dimensions of the given [localSpace].
 *
 * @param localSpace The coordinate space whose width and height define the scaling factors.
 * @return The converted [ImagePoint] in absolute pixel coordinates.
 */
fun NormImagePoint.toPoint(localSpace: ImageSpace): ImagePoint = normPointToPoint(this, localSpace)
