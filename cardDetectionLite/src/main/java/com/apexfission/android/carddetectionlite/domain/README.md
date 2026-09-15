# `com.apexfission.android.carddetectionlite.domain`

Source set: `main` · Module: [`:cardDetectionLite`](../../../../../../../../docs/README.md)

## Contribution

Provides the model-catalog extension point so trained assets can live outside the core library.

## Responsibilities and boundaries

ModelCatalog.TfLite.Companion is a namespace, not a loader. The tfmodel module supplies imported extension properties for the bundled asset.

## Source files

- [ModelCatalog.kt](ModelCatalog.kt)
