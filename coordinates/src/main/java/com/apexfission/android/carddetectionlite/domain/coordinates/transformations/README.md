# `com.apexfission.android.carddetectionlite.domain.coordinates.transformations`

Source set: `main` · Module: [`:coordinates`](../../../../../../../../../../docs/README.md)

## Contribution

Moves points and boxes between parent/child spaces and across directional chains.

## Responsibilities and boundaries

translate follows chain relationships, applying scale and offsets. Normalized helpers interpret local-space values first. Parent mapping validates bounds and child mapping clamps to the child extent. These are geometry operations, not bitmap rotation/resizing.

## Source files

- [box.kt](box.kt)
- [normBox.kt](normBox.kt)
- [normPoint.kt](normPoint.kt)
- [point.kt](point.kt)
- [src.kt](src.kt)
