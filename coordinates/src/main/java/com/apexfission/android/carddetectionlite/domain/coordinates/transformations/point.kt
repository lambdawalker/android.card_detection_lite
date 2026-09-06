package com.apexfission.android.carddetectionlite.domain.coordinates.transformations

import com.apexfission.android.carddetectionlite.domain.coordinates.models.ImagePoint
import com.apexfission.android.carddetectionlite.domain.coordinates.models.ImageSpace
import com.apexfission.android.carddetectionlite.domain.coordinates.models.ImageSpaceChain


/**
 * Extension function to transform this [ImagePoint] from a [childSpace] frame to a [parentSpace] frame.
 *
 * @param parentSpace The target parent coordinate space.
 * @param childSpace The source child coordinate space.
 * @return The transformed [ImagePoint] in parent space.
 * @throws IllegalArgumentException If resulting coordinates are negative or exceed parent space bounds.
 */
fun ImagePoint.toParentSpace(parentSpace: ImageSpace, childSpace: ImageSpace): ImagePoint =
    pointToParentSpace(x = x, y = y, childSpace = childSpace, parentSpace = parentSpace)


fun ImagePoint.toParentSpace(chain: ImageSpaceChain): ImagePoint =
    chain.reversed().zipWithNext().fold(this) { point, (child, parent) ->
        pointToParentSpace(point.x, point.y, child, parent)
    }

/**
 * Extension function to transform this [ImagePoint] from a [parentSpace] frame to a [childSpace] frame.
 *
 * @param parentSpace The source parent coordinate space.
 * @param childSpace The target child coordinate space.
 * @return The transformed [ImagePoint] in child space.
 * @throws IllegalArgumentException If resulting coordinates are negative or exceed child space bounds.
 */
fun ImagePoint.toChildSpace(parentSpace: ImageSpace, childSpace: ImageSpace): ImagePoint =
    pointToChildSpace(x = x, y = y, parentSpace = parentSpace, childSpace = childSpace)

fun ImagePoint.toChildSpace(chain: ImageSpaceChain): ImagePoint =
    chain.zipWithNext().fold(this) { point, (parent, child) ->
        pointToChildSpace(point.x, point.y, parent, child)
    }
