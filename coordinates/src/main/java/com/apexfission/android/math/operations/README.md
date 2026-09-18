# `com.apexfission.android.carddetectionlite.domain.coordinates.operations`

Source set: `main` · Module: [`:coordinates`](../../../../../../../../../../docs/README.md)

## Contribution

Converts normalized points and boxes to pixels within one image space.

## Responsibilities and boundaries

NormImagePoint.toPoint and NormImageBox.toBox denormalize values. They do not traverse scale/crop chains; that belongs to transformations.

## Source files

- [normBox.kt](normBox.kt)
- [normPoint.kt](normPoint.kt)
