package com.apexfission.android.carddetectionlite.domain.coordinates.transformations

import com.apexfission.android.carddetectionlite.domain.coordinates.ImageBox2P
import com.apexfission.android.carddetectionlite.domain.coordinates.ImageSpace
import com.apexfission.android.carddetectionlite.domain.coordinates.NormImageBox2P
import com.apexfission.android.carddetectionlite.domain.coordinates.NormImageBoxPS
import com.apexfission.android.carddetectionlite.domain.coordinates.operations.toBox2P
import com.apexfission.android.carddetectionlite.domain.coordinates.operations.toBoxPS
import com.apexfission.android.carddetectionlite.domain.coordinates.operations.toImageBox2p

/**
 * Extension function to transform a normalized point-size bounding box [NormImageBoxPS]
 * from a [childSpace] frame into an absolute [ImageBox2P] in a [parentSpace] frame.
 *
 * @param normImageBoxPS The normalized point-size bounding box in child space.
 * @param parentSpace The target parent coordinate space.
 * @param childSpace The source child coordinate space.
 * @return The transformed [ImageBox2P] in parent space.
 */
fun NormImageBoxPS.toParentSpace(normImageBoxPS: NormImageBoxPS, parentSpace: ImageSpace, childSpace: ImageSpace): ImageBox2P =
    normImageBoxPS.toBoxPS(childSpace).toImageBox2p().toParentSpace(parentSpace, childSpace)

/**
 * Extension function to transform a normalized two-point bounding box [NormImageBox2P]
 * from a [childSpace] frame into an absolute [ImageBox2P] in a [parentSpace] frame.
 *
 * @param normImageBox2P The normalized bounding box in child space.
 * @param parentSpace The target parent coordinate space.
 * @param childSpace The source child coordinate space.
 * @return The transformed [ImageBox2P] in parent space.
 */
fun NormImageBox2P.toParentSpace(normImageBox2P: NormImageBox2P, parentSpace: ImageSpace, childSpace: ImageSpace): ImageBox2P =
    normImageBox2P.toBox2P(childSpace).toParentSpace(parentSpace, childSpace)
