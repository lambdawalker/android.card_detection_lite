# Geometry and image spaces

[Documentation index](README.md)

## Types

| Type | Meaning |
| --- | --- |
| ImagePoint / ImageBox | Absolute pixel geometry stored with UInt coordinates. |
| NormImagePoint / NormImageBox | Floating normalized geometry, interpreted relative to a specified space. |
| ImageSpace | Dimensions plus scale and offset relative to another space. |
| ImageSpaceChainNode | An image space and its Parent/Child relationship to the preceding node. |
| ImageSpaceChain | A list-like wrapper of directional nodes, not a typealias for List<ImageSpace>. |

Use box builders such as `ImageBox.from2P` and `ImageBox.fromPS`. Use `ImageSpace.scale`, `crop`, and `cropAtCenter` to describe geometry changes. Those operations do not alter any bitmap.

## Construct and apply a chain

This complete function maps a box from a 1920 × 1080 source into a centered 1080 × 1080 crop:

```kotlin
import com.apexfission.android.carddetectionlite.domain.coordinates.models.ImageBox
import com.apexfission.android.carddetectionlite.domain.coordinates.models.ImageSpace
import com.apexfission.android.carddetectionlite.domain.coordinates.models.chain
import com.apexfission.android.carddetectionlite.domain.coordinates.models.cropAtCenter
import com.apexfission.android.carddetectionlite.domain.coordinates.transformations.translate

fun exampleViewportBox(): ImageBox {
    val source = ImageSpace(width = 1920u, height = 1080u)
    val viewport = source.cropAtCenter(width = 1080u, height = 1080u)
    val chain = source.chain(viewport)
    val sourceBox = ImageBox.from2P(500u, 100u, 1000u, 600u)
    return sourceBox.translate(chain) // (80, 100) to (580, 600)
}
```

`chain` defaults to a Child relationship. Specify `SpaceRelationship.Parent` when moving toward a parent. An existing `List<ImageSpace>` can be converted through `toImageSpaceChain()` when all steps represent child relationships.

## Conversion versus transformation

`NormImageBox.toBox(space)` and `NormImagePoint.toPoint(space)`, imported from operations, only denormalize within that space. They do not account for a preview crop or preceding scales.

Transformation helpers apply offsets/scales between spaces; `translate(chain)` traverses directional nodes. Source-to-child mapping subtracts child offsets and applies child scale; child-to-parent mapping reverses that operation. Parent mapping validates bounds; child mapping clamps to the child extent.

Unsigned coordinates cannot represent negative model predictions. Validate/clamp signed calculations before conversion. Do not treat normalized construction as proof that arbitrary model output is valid.

Camera rotation and bitmap cropping belong to the core image package. Preview coordinate chains belong to the camera/simulation adapters. Keep the coordinate space of a box explicit at each boundary.
