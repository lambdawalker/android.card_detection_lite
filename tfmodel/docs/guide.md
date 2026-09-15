# Bundled model and catalog

[Documentation index](README.md)

The active model is `cdl/tflite/Y11-640E197F16.tflite`, under the module's [assets directory](../src/main/assets/cdl/tflite). The catalog's modelName is `Y11-640E197F16.tflite`. The extension source is [add.kt](../src/main/java/com/apexfission/android/carddetectionlite/tfmodel/add.kt).

| Class ID | Label | Primary card class |
| --- | --- | --- |
| 0 | horizontal_card | Yes |
| 1 | vertical_card | Yes |
| 2 | horizontal_card_back | Yes |
| 3 | photo | No |
| 4 | slim-barcode | No |
| 5 | pdf417 | No |
| 6 | mrz-text | No |
| 7 | barcode | No |
| 8 | qrcode | No |

`cardClasses` is `Set<Int>` containing 0, 1, and 2. Other classes describe features, not OCR text or decoded barcode values.

## Importing metadata

Add project dependencies on core and tfmodel in the host application, then import the extensions:

```kotlin
import com.apexfission.android.carddetectionlite.domain.ModelCatalog
import com.apexfission.android.carddetectionlite.tfmodel.cardClasses
import com.apexfission.android.carddetectionlite.tfmodel.classes
import com.apexfission.android.carddetectionlite.tfmodel.modelPath

val assetPath = ModelCatalog.TfLite.modelPath
val labels = ModelCatalog.TfLite.classes
val primaryClasses = ModelCatalog.TfLite.cardClasses
```

Pass these values to CardDetectorLite, CardTrackingSimulator, or lower-level builders. The extensions expose metadata; they do not load or execute the model.

## Replacing the model

Keep asset path, output class ordering, labels, and cardClasses aligned. The core validates a specific square-RGB YOLO tensor contract; see [supported contract](../../cardDetectionLite/docs/architecture.md). FP16-weight naming does not imply FLOAT16 input/output support.

Preserve uncompressed tflite packaging and ensure Git LFS assets are hydrated when building from a checkout. Validate new assets using real inference tests, not only catalog changes. This repository module contains runtime assets/metadata rather than a model-training pipeline.
