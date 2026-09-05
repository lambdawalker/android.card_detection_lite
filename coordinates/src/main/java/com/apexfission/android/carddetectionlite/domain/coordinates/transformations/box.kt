package com.apexfission.android.carddetectionlite.domain.coordinates.transformations

import com.apexfission.android.carddetectionlite.domain.coordinates.ImageBox2P
import com.apexfission.android.carddetectionlite.domain.coordinates.ImageSpace
import com.apexfission.android.carddetectionlite.domain.coordinates.operations.arrange

/**
 * Transforms an [ImageBox2P] bounding box from a [childSpace] frame to a [parentSpace] frame.
 *
 * @param imageBox2P The bounding box in child space.
 * @param parentSpace The target parent coordinate space configuration.
 * @param childSpace The source child coordinate space configuration.
 * @return The transformed [ImageBox2P] in parent space.
 */
fun imageBox2PToParentSpace(imageBox2P: ImageBox2P, parentSpace: ImageSpace, childSpace: ImageSpace): ImageBox2P {
    val imageBox2P = imageBox2P.arrange()
    val pointA = pointToParentSpace(imageBox2P.x, imageBox2P.y, childSpace, parentSpace)
    val pointB = pointToParentSpace(imageBox2P.x2, imageBox2P.y2, childSpace, parentSpace)

    return ImageBox2P(
        x = pointA.x, y = pointA.y, x2 = pointB.x, y2 = pointB.y
    )
}

/**
 * Extension function to transform this [ImageBox2P] bounding box from a [childSpace] frame to a [parentSpace] frame.
 *
 * @param parentSpace The target parent coordinate space configuration.
 * @param childSpace The source child coordinate space configuration.
 * @return The transformed [ImageBox2P] in parent space.
 */
fun ImageBox2P.toParentSpace(parentSpace: ImageSpace, childSpace: ImageSpace): ImageBox2P = imageBox2PToParentSpace(this, parentSpace, childSpace)

/**
 * Transforms an [ImageBox2P] bounding box from a [parentSpace] frame to a [childSpace] frame.
 *
 * @param imageBox2P The bounding box in parent space.
 * @param parentSpace The source parent coordinate space configuration.
 * @param childSpace The target child coordinate space configuration.
 * @return The transformed [ImageBox2P] in child space.
 */
fun imageBox2PToChildSpace(imageBox2P: ImageBox2P, parentSpace: ImageSpace, childSpace: ImageSpace): ImageBox2P {
    val imageBox2P = imageBox2P.arrange()
    val pointA = pointToChildSpace(imageBox2P.x, imageBox2P.y, parentSpace, childSpace)
    val pointB = pointToChildSpace(imageBox2P.x2, imageBox2P.y2, parentSpace, childSpace)

    return ImageBox2P(
        x = pointA.x, y = pointA.y, x2 = pointB.x, y2 = pointB.y
    )
}

/**
 * Extension function to transform this [ImageBox2P] bounding box from a [parentSpace] frame to a [childSpace] frame.
 *
 * @param parentSpace The source parent coordinate space configuration.
 * @param childSpace The target child coordinate space configuration.
 * @return The transformed [ImageBox2P] in child space.
 */
fun ImageBox2P.toChildSpace(parentSpace: ImageSpace, childSpace: ImageSpace): ImageBox2P = imageBox2PToChildSpace(this, parentSpace, childSpace)
