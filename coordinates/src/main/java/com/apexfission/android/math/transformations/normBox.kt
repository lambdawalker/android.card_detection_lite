package com.apexfission.android.math.transformations

import com.apexfission.android.math.models.ImageBox
import com.apexfission.android.math.models.ImageSpace
import com.apexfission.android.math.models.NormImageBox
import com.apexfission.android.math.operations.toBox

/**
 * Transforms a normalized bounding box [NormImageBox] from a [childSpace] frame
 * into an absolute [ImageBox] in a [parentSpace] frame.
 *
 * @param normImageBox The normalized bounding box in child space.
 * @param parentSpace The target parent coordinate space.
 * @param childSpace The source child coordinate space.
 * @return The transformed [ImageBox] in parent space.
 */
fun normBoxToParentSpace(normImageBox: NormImageBox, parentSpace: ImageSpace, childSpace: ImageSpace): ImageBox =
    normImageBox.toBox(childSpace).toParentSpace(parentSpace, childSpace)

/**
 * Extension function to transform this normalized bounding box [NormImageBox]
 * from a [childSpace] frame into an absolute [ImageBox] in a [parentSpace] frame.
 *
 * @param parentSpace The target parent coordinate space.
 * @param childSpace The source child coordinate space.
 * @return The transformed [ImageBox] in parent space.
 */
fun NormImageBox.toParentSpace(parentSpace: ImageSpace, childSpace: ImageSpace): ImageBox =
    normBoxToParentSpace(this, parentSpace, childSpace)
