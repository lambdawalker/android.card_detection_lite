package com.apexfission.android.carddetectionlite.domain.coordinates.operations

import com.apexfission.android.carddetectionlite.domain.coordinates.ImageBox2P
import com.apexfission.android.carddetectionlite.domain.coordinates.ImageBoxPS
import kotlin.math.max
import kotlin.math.min

/**
 * Arranges an [ImageBox2P] to ensure `(x, y)` represents the top-left corner (min values)
 * and `(x2, y2)` represents the bottom-right corner (max values).
 *
 * @param box The bounding box to arrange.
 * @return A new [ImageBox2P] with arranged min/max coordinates.
 */
fun arrangeImageBox2P(box: ImageBox2P): ImageBox2P = ImageBox2P(
    x = min(box.x, box.x2),
    y = min(box.y, box.y2),
    x2 = max(box.x, box.x2),
    y2 = max(box.y, box.y2)
)

/**
 * Extension function to arrange this [ImageBox2P] so `(x, y)` is top-left and `(x2, y2)` is bottom-right.
 *
 * @return A new arranged [ImageBox2P].
 */
fun ImageBox2P.arrange(): ImageBox2P = arrangeImageBox2P(this)

/**
 * Converts an [com.apexfission.android.carddetectionlite.domain.coordinates.ImageBoxPS] (point + size) into an [ImageBox2P] (two points: top-left and bottom-right).
 *
 * @param box The point-size bounding box to convert.
 * @return The equivalent [ImageBox2P].
 */
fun imageBoxPStoImageBox2p(box: ImageBoxPS): ImageBox2P = ImageBox2P(
    x = box.x,
    y = box.y,
    x2 = (box.x.toInt() + box.width).toUInt(),
    y2 = (box.y.toInt() + box.height).toUInt()
)

/**
 * Extension function to convert this [ImageBoxPS] into an [ImageBox2P].
 *
 * @return The equivalent [ImageBox2P].
 */
@JvmName("toImageBox2pExt")
fun ImageBoxPS.toImageBox2p(): ImageBox2P = imageBoxPStoImageBox2p(this)
