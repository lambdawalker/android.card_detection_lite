# `com.apexfission.android.carddetectionlite.tfmodel`

Source set: `main` · Module: [`:tfmodel`](../../../../../../../../docs/README.md)

## Contribution

Supplies the bundled Sentinel model's catalog extensions.

## Responsibilities and boundaries

add.kt extends ModelCatalog.TfLite.Companion with modelPath, modelName, classes, and cardClasses. Import these extensions explicitly. Assets live under assets/cdl/tflite. This package does not train models or run inference.

## Source files

- [add.kt](add.kt)
