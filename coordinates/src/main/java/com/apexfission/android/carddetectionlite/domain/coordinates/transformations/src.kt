package com.apexfission.android.carddetectionlite.domain.coordinates.transformations

import com.apexfission.android.carddetectionlite.domain.coordinates.models.ImagePoint
import com.apexfission.android.carddetectionlite.domain.coordinates.models.ImageSpace

/**
 * Transforms point coordinates `(x, y)` from a [childSpace] (Local) frame to a [parentSpace] (Original/Global) frame.
 * Uses Double precision for internal arithmetic before converting to [UInt].
 *
 * @param x The X-coordinate in child space.
 * @param y The Y-coordinate in child space.
 * @param childSpace The child coordinate space configuration.
 * @param parentSpace The parent coordinate space configuration.
 * @return The transformed [ImagePoint] in parent space.
 * @throws IllegalArgumentException If resulting coordinates are negative or exceed parent space bounds.
 */
fun pointToParentSpace(x: UInt, y: UInt, childSpace: ImageSpace, parentSpace: ImageSpace): ImagePoint {
    val parentX = x.toDouble() / childSpace.xScale.toDouble() + childSpace.xOffset.toDouble()
    val parentY = y.toDouble() / childSpace.yScale.toDouble() + childSpace.yOffset.toDouble()

    require(parentX >= 0.0 && parentY >= 0.0) { "InvalidCoordinatesError: Resulting spatial coordinates cannot be negative." }
    require(parentX <= parentSpace.width.toDouble() && parentY <= parentSpace.height.toDouble()) { "InvalidCoordinatesError: Resulting spatial coordinates cannot exceed parent space dimensions." }

    return ImagePoint(
        x = parentX.toUInt(), y = parentY.toUInt()
    )
}

/**
 * Transforms point coordinates `(x, y)` from a [parentSpace] (Original/Global) frame to a [childSpace] (Local) frame.
 * Uses Double precision for internal arithmetic before converting to [UInt].
 *
 * @param x The X-coordinate in parent space.
 * @param y The Y-coordinate in parent space.
 * @param parentSpace The parent coordinate space configuration.
 * @param childSpace The child coordinate space configuration.
 * @return The transformed [ImagePoint] in child space.
 * @throws IllegalArgumentException If resulting coordinates are negative or exceed child space bounds.
 */
fun pointToChildSpace(x: UInt, y: UInt, parentSpace: ImageSpace, childSpace: ImageSpace): ImagePoint {
    val childX = (x.toDouble() - childSpace.xOffset.toDouble()) * childSpace.xScale.toDouble()
    val childY = (y.toDouble() - childSpace.yOffset.toDouble()) * childSpace.yScale.toDouble()

    return ImagePoint(
        x = childX.toUInt().coerceIn(0u, childSpace.width),
        y = childY.toUInt().coerceIn(0u, childSpace.height)
    )
}
