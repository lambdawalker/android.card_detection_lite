# :tfmodel Module

`tfmodel` is an asset and metadata provider module containing the trained TensorFlow Lite card detection model file (`Y11-640E197F16.tflite`) and extension catalog properties.

---

## Model Asset Details

- **Asset Path**: `cdl/tflite/Y11-640E197F16.tflite`
- **Architecture**: Modified YOLOv11 optimized for mobile TFLite inference.
- **Precision**: Float16 / FP16 quantized weights.
- **Model Size**: ~5.3 MB.
- **Target Classes**: ID Cards, Driver's Licenses, Voter IDs, and document subfeatures (photo, barcode, text region).

---

## Extension Catalog Properties

`tfmodel` extends `ModelCatalog.TfLite.Companion` from `:cardDetectionLite` with concrete asset constants:

- **`ModelCatalog.TfLite.modelPath`**: `"cdl/tflite/Y11-640E197F16.tflite"`
- **`ModelCatalog.TfLite.classes`**: `Map<Int, String>` mapping class IDs to human-readable labels.
- **`ModelCatalog.TfLite.cardClasses`**: `Set<Int>` defining primary target card class IDs.

---

## Integration

### `build.gradle.kts`
```kotlin
dependencies {
    implementation(project(":cardDetectionLite"))
    implementation(project(":tfmodel"))
}
```

---

## Usage Example

```kotlin
val modelPath: String = ModelCatalog.TfLite.modelPath
val classLabels: Map<Int, String> = ModelCatalog.TfLite.classes
val cardClasses: Set<Int> = ModelCatalog.TfLite.cardClasses

CardDetectorLite(
    instanceKey = "main-camera",
    modelPath = modelPath,
    classLabels = classLabels,
    cardClasses = cardClasses,
    onCardDetection = { detection, bitmap ->
        // The caller owns bitmap and must recycle it after its final use.
    }
)
```
