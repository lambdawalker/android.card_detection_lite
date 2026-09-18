package com.apexfission.android.math.transformations

import com.apexfission.android.math.models.ImagePoint
import com.apexfission.android.math.models.ImageSpace
import com.apexfission.android.math.models.NormImagePoint
import com.apexfission.android.math.operations.toPoint

/**
 * Extension function to transform a normalized point [NormImagePoint]
 * from a [childSpace] frame into an absolute [ImagePoint] in a [parentSpace] frame.
 *
 * @param parentSpace The target parent coordinate space.
 * @param childSpace The source child coordinate space.
 * @return The transformed [ImagePoint] in parent space.
 */
fun NormImagePoint.toParentSpace(parentSpace: ImageSpace, childSpace: ImageSpace): ImagePoint =
    this.toPoint(childSpace).toParentSpace(parentSpace, childSpace)

/**
 * Extension function to transform a normalized point [NormImagePoint]
 * from a [parentSpace] frame into an absolute [ImagePoint] in a [childSpace] frame.
 *
 * @param parentSpace The source parent coordinate space.
 * @param childSpace The target child coordinate space.
 * @return The transformed [ImagePoint] in child space.
 */
fun NormImagePoint.toChildSpace(parentSpace: ImageSpace, childSpace: ImageSpace): ImagePoint =
    this.toPoint(childSpace).toChildSpace(parentSpace, childSpace)
