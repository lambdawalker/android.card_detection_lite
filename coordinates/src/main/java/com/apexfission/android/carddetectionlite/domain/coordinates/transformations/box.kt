package com.apexfission.android.carddetectionlite.domain.coordinates.transformations

import com.apexfission.android.carddetectionlite.domain.coordinates.models.ImageBox
import com.apexfission.android.carddetectionlite.domain.coordinates.models.ImagePoint
import com.apexfission.android.carddetectionlite.domain.coordinates.models.ImageSpace
import com.apexfission.android.carddetectionlite.domain.coordinates.models.ImageSpaceChain


/**
 * Extension function to transform this [ImageBox] bounding box from a [childSpace] frame to a [parentSpace] frame.
 *
 * @param parentSpace The target parent coordinate space configuration.
 * @param childSpace The source child coordinate space configuration.
 * @return The transformed [ImageBox] in parent space.
 */
fun ImageBox.toParentSpace(parentSpace: ImageSpace, childSpace: ImageSpace): ImageBox {
    val pointA = pointToParentSpace(x, y, childSpace, parentSpace)
    val pointB = pointToParentSpace(x2, y2, childSpace, parentSpace)

    return ImageBox.from2P(
        x1 = pointA.x, y1 = pointA.y, x2 = pointB.x, y2 = pointB.y
    )
}


/**
 * Extension function to transform this [ImageBox] bounding box from a [parentSpace] frame to a [childSpace] frame.
 *
 * @param parentSpace The source parent coordinate space configuration.
 * @param childSpace The target child coordinate space configuration.
 * @return The transformed [ImageBox] in child space.
 */
fun ImageBox.toChildSpace(parentSpace: ImageSpace, childSpace: ImageSpace): ImageBox {
    val pointA = pointToChildSpace(x, y, parentSpace, childSpace)
    val pointB = pointToChildSpace(x2, y2, parentSpace, childSpace)

    return ImageBox.from2P(
        x1 = pointA.x, y1 = pointA.y, x2 = pointB.x, y2 = pointB.y
    )
}

fun ImageBox.toChildSpace(imageSpaceChain: ImageSpaceChain): ImageBox {
    val pointA = ImagePoint(x, y).toChildSpace(imageSpaceChain)
    val pointB = ImagePoint(x2, y2).toChildSpace(imageSpaceChain)

    return ImageBox.from2P(
        x1 = pointA.x, y1 = pointA.y, x2 = pointB.x, y2 = pointB.y
    )
}
