# `com.apexfission.android.carddetectionlite.domain.coordinates.models`

Source set: `main` · Module: [`:coordinates`](../../../../../../../../../../docs/README.md)

## Contribution

Defines pixel/normalized geometry and directional image-space chains.

## Responsibilities and boundaries

ImageSpaceChain wraps ImageSpaceChainNode values with Parent/Child relationships; it is not a List<ImageSpace> alias. Use chain or toImageSpaceChain. Pixel coordinates use UInt: validate signed/floating calculations before conversion. Compose annotations are compile-only dependencies.

## Source files

- [ImageBox.kt](ImageBox.kt)
- [ImagePoint.kt](ImagePoint.kt)
- [ImageSpace.kt](ImageSpace.kt)
- [NormImageBox.kt](NormImageBox.kt)
- [NormImagePoint.kt](NormImagePoint.kt)
