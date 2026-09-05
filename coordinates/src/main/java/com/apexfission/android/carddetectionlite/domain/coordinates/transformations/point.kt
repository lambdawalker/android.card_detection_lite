package com.apexfission.android.carddetectionlite.domain.coordinates.transformations

import com.apexfission.android.carddetectionlite.domain.coordinates.ImagePoint
import com.apexfission.android.carddetectionlite.domain.coordinates.ImageSpace
import com.apexfission.android.carddetectionlite.domain.coordinates.ImageSpaceChain


/**
 * Transforms an [ImagePoint] from a [childSpace] (Local) frame to a [parentSpace] (Original/Global) frame.
 *
 * @param point The point in child space.
 * @param childSpace The child coordinate space configuration.
 * @param parentSpace The parent coordinate space configuration.
 * @return The transformed [ImagePoint] in parent space.
 * @throws IllegalArgumentException If resulting coordinates are negative or exceed parent space bounds.
 */
fun pointToParentSpace(point: ImagePoint, childSpace: ImageSpace, parentSpace: ImageSpace): ImagePoint =
    pointToParentSpace(x = point.x, y = point.y, childSpace = childSpace, parentSpace = parentSpace)

/**
 * Extension function to transform this [ImagePoint] from a [childSpace] frame to a [parentSpace] frame.
 *
 * @param parentSpace The target parent coordinate space.
 * @param childSpace The source child coordinate space.
 * @return The transformed [ImagePoint] in parent space.
 * @throws IllegalArgumentException If resulting coordinates are negative or exceed parent space bounds.
 */
fun ImagePoint.toParentSpace(parentSpace: ImageSpace, childSpace: ImageSpace): ImagePoint =
    pointToParentSpace(point = this, childSpace = childSpace, parentSpace = parentSpace)

fun pointToParentSpace(point: ImagePoint, chain: ImageSpaceChain): ImagePoint =
    chain.zipWithNext().fold(point) { point, (parent, child) ->
        pointToParentSpace(point.x, point.y, parent, child)
    }


fun ImagePoint.toParentSpace(chain: ImageSpaceChain): ImagePoint = pointToParentSpace(this, chain)



/**
 * Transforms an [ImagePoint] from a [parentSpace] (Original/Global) frame to a [childSpace] (Local) frame.
 *
 * @param point The point in parent space.
 * @param parentSpace The parent coordinate space configuration.
 * @param childSpace The child coordinate space configuration.
 * @return The transformed [ImagePoint] in child space.
 * @throws IllegalArgumentException If resulting coordinates are negative or exceed child space bounds.
 */
fun pointToChildSpace(point: ImagePoint, parentSpace: ImageSpace, childSpace: ImageSpace): ImagePoint =
    pointToChildSpace(x = point.x, y = point.y, parentSpace = parentSpace, childSpace = childSpace)

/**
 * Extension function to transform this [ImagePoint] from a [parentSpace] frame to a [childSpace] frame.
 *
 * @param parentSpace The source parent coordinate space.
 * @param childSpace The target child coordinate space.
 * @return The transformed [ImagePoint] in child space.
 * @throws IllegalArgumentException If resulting coordinates are negative or exceed child space bounds.
 */
fun ImagePoint.toChildSpace(parentSpace: ImageSpace, childSpace: ImageSpace): ImagePoint =
    pointToChildSpace(point = this, parentSpace = parentSpace, childSpace = childSpace)

