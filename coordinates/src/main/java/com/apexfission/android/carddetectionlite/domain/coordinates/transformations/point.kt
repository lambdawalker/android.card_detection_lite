package com.apexfission.android.carddetectionlite.domain.coordinates.transformations

import com.apexfission.android.carddetectionlite.domain.coordinates.models.ImagePoint
import com.apexfission.android.carddetectionlite.domain.coordinates.models.ImageSpace
import com.apexfission.android.carddetectionlite.domain.coordinates.models.ImageSpaceChain
import com.apexfission.android.carddetectionlite.domain.coordinates.models.SpaceRelationship
import com.apexfission.android.carddetectionlite.domain.coordinates.models.toImageSpaceChain


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


fun ImagePoint.toParentSpace(spaces: List<ImageSpace>): ImagePoint {
    return spaces.reversed().zipWithNext().fold(this) { point, (child, parent) ->
        pointToParentSpace(point.x, point.y, child, parent)
    }
}

fun ImagePoint.toParentSpace(chain: ImageSpaceChain): ImagePoint = translate(chain)

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

fun ImagePoint.toChildSpace(spaces: List<ImageSpace>): ImagePoint {
    val chain = spaces.toImageSpaceChain()
    return translate(chain)
}

fun ImagePoint.toChildSpace(chain: ImageSpaceChain): ImagePoint = translate(chain)

/**
 * Translates this [ImagePoint] through an [ImageSpaceChain] of connected spaces and relationships.
 */
fun ImagePoint.translate(chain: ImageSpaceChain): ImagePoint {
    require(chain.isNotEmpty()) { "ImageSpaceChain cannot be empty" }
    var currentPoint = this
    var currentSpace = chain.first().space

    for (i in 1 until chain.size) {
        val node = chain[i]
        val nextSpace = node.space
        currentPoint = when (node.relationship) {
            SpaceRelationship.Parent -> pointToParentSpace(currentPoint.x, currentPoint.y, currentSpace, nextSpace)
            SpaceRelationship.Child -> pointToChildSpace(currentPoint.x, currentPoint.y, currentSpace, nextSpace)
        }
        currentSpace = nextSpace
    }
    return currentPoint
}
