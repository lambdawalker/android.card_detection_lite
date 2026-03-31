# cardDetectionLite Module

## Overview

`cardDetectionLite` is a real-time Jetpack Compose module for detecting government-issued ID cards, such as driver’s licenses, voter IDs, and similar documents, directly from the camera feed.

It uses a YOLO-based TensorFlow Lite model to detect the card in view, stabilize the detection across frames, and return a cropped card image along with any detected subfeatures inside the card area.

This module is designed as the **first stage** of an ID-processing pipeline. It focuses on **detection and extraction only**.

> `cardDetectionLite` does **not** perform OCR.
>
> Its role is to find the card, isolate it from the live camera stream, and return the cropped result so it can be passed to a separate OCR or data-extraction component.

The main entry point is the `CardDetectorLite` composable, which bundles camera preview, model inference, lock-on logic, and optional visual overlays into a single configurable UI component.

---

## Getting Started

### Dependencies

Add the module and model dependencies to your `build.gradle.kts` file:

```kotlin
dependencies {
    implementation("com.apexfission.android.carddetectionlite:core:0.0.1-A1")
    implementation("com.apexfission.android.carddetectionlite:tfmodel-Y11-640-F16:0.0.1-A1")
}
```

---

## Basic Usage

### 1. Declare camera permission in `AndroidManifest.xml`

```xml
<uses-feature
    android:name="android.hardware.camera.any"
    android:required="true" />
<uses-permission android:name="android.permission.CAMERA" />
```

### 2. Manage permission and detection state

Use the provided `HandleCameraPermission` composable and a `ViewModel` to control when detection is active.

```kotlin
class MainViewModel : ViewModel() {
    private val _isDetectionEnabled = MutableStateFlow(true)
    val isDetectionEnabled = _isDetectionEnabled.asStateFlow()

    fun onDetections(card: CardDetection) {
        _isDetectionEnabled.value = false

        viewModelScope.launch {
            try {
                Log.d("CardDetection", "Card detected: ${card.id}")
            } finally {
                delay(2000)
                _isDetectionEnabled.value = true
            }
        }
    }
}
```

### 3. Add `CardDetectorLite` to your UI

```kotlin
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val mainViewModel: MainViewModel by viewModels()

            HandleCameraPermission {
                val isDetectionEnabled by mainViewModel
                    .isDetectionEnabled
                    .collectAsStateWithLifecycle()

                CardDetectorLite(
                    modelPath = ModelCatalog.TfLite.modelPath,
                    classLabels = ModelCatalog.TfLite.classes,
                    cardClasses = ModelCatalog.TfLite.cardClasses,
                    isDetectionEnabled = isDetectionEnabled,
                    onCardDetection = { card ->
                        mainViewModel.onDetections(card)
                    }
                )
            }
        }
    }
}
```

---

## `CardDetection` Result

The `onCardDetection` callback returns a `CardDetection` object with the following properties:

- **`id: Int?`**\
  A unique identifier assigned when a card becomes stable enough to be considered locked on. This value is `null` until lock-on completes.

- **`card: ExtractedFeature`**\
  The primary detected card. It includes the bounding box, cropped `Bitmap`, and confidence score.

- **`features: List<ExtractedFeature>`**\
  Additional detected items found inside the main card region, such as photos, or barcodes.

- **`lockOnProgress: Float`**\
  A value from `0.0` to `1.0` representing how close the detection is to becoming stable. A value of `1.0` means the card is fully locked on.

- **`isNewDetection: Boolean`**\
  `true` only on the first frame where a stable lock-on is achieved. Useful for triggering one-time events such as sound, haptics, or navigation.

- **`contextSize: Rect`**\
  The dimensions of the analyzed image context. This is useful when translating detection coordinates to screen coordinates.

---

## Model Details

- **Architecture**: Modified YOLOv11 optimized for mobile TensorFlow Lite inference
- **Training Data**: Private synthetic dataset with more than 20,000 examples across multiple ID card types
- **Model Size**: Approximately 5.3 MB

This balance keeps the model lightweight enough for mobile use while still supporting real-time detection scenarios.

---

## Configuration

`CardDetectorLite` supports the following parameters:

| Parameter                  | Type                      | Default                                             | Description                                                                         |
| -------------------------- | ------------------------- | --------------------------------------------------- | ----------------------------------------------------------------------------------- |
| `modelPath`                | `String`                  | -                                                   | Path to the `.tflite` model inside the app assets.                                  |
| `classLabels`              | `Map<Int, String>`        | -                                                   | Maps class IDs to human-readable labels.                                            |
| `cardClasses`              | `List<Int>`               | -                                                   | Class IDs that should be treated as primary card targets.                           |
| `isDetectionEnabled`       | `Boolean`                 | -                                                   | Enables or pauses detection dynamically.                                            |
| `onCardDetection`          | `(CardDetection) -> Unit` | -                                                   | Called when a stable card lock-on is achieved.                                      |
| `modifier`                 | `Modifier`                | `Modifier`                                          | Modifier applied to the root container.                                             |
| `useGpu`                   | `Boolean`                 | `true`                                              | Enables TFLite GPU delegate when available.                                         |
| `showBoundingBoxes`        | `Boolean`                 | `false`                                             | Draws bounding boxes for detected objects.                                          |
| `showClassNames`           | `Boolean`                 | `false`                                             | Shows class labels and confidence values above boxes.                               |
| `showFlashlightSwitch`     | `Boolean`                 | `true`                                              | Shows a built-in flashlight toggle button.                                          |
| `showLockOnProgress`       | `Boolean`                 | `true`                                              | Displays visual feedback while lock-on stabilizes.                                  |
| `scoreThreshold`           | `Float`                   | `0.65f`                                             | Minimum confidence required for a detection to be considered valid.                 |
| `analysisTargetResolution` | `Size`                    | `Size(2048, 1080)`                                  | Resolution requested for the analysis stream.                                       |
| `cardCardFilters`          | `List<CardValidator>`     | `listOf(MarginValidator(), AspectRatioValidator())` | Heuristic validators applied to candidate card detections.                          |
| `imageMode`                | `InputShape`              | `InputShape.SquareCrop`                             | Defines how the camera image is prepared before inference.                          |
| `inferenceIntervalMs`      | `Long`                    | `33L`                                               | Minimum interval between inference runs.                                            |
| `tapToFocusEnabled`        | `Boolean`                 | `true`                                              | Enables tap-to-focus on the preview.                                                |
| `focusOnCardEnabled`       | `Boolean`                 | `true`                                              | Enables auto-focus behavior targeting the detected card.                            |
| `lockOnThreshold`          | `Int`                     | `5`                                                 | Number of consistent frames required before lock-on succeeds.                       |
| `numThreads`               | `Int?`                    | `null`                                              | Number of CPU threads used for inference. Defaults to `max(2, num_processors / 2)`. |

> Note: The parameter name `cardCardFilters` appears unusual and may be intentional for backward compatibility. If this is not required by the API, consider renaming it to `cardFilters` for clarity.

---

## Understanding Output Card Resolution

The cropped card image returned through `onCardDetection` does **not** have a fixed resolution. Its quality depends on several factors:

### 1. `analysisTargetResolution`

This determines the resolution of the camera image used for inference and extraction. Higher values generally allow better card crops, but they also increase processing cost.

### 2. Physical distance to the card

This is usually the biggest factor. A card that occupies more of the camera frame will produce a higher-resolution crop than one captured from farther away.

### 3. `imageMode`

The preprocessing mode determines which part of the camera frame is analyzed:

- With `FullImage`, the card may occupy a smaller portion of the analyzed frame but will be found throughout the whole image.
- With crop-based modes such as `SquareCrop`, the card can fill more of the model input when centered properly, improving effective resolution.

### Best-practice recommendation

For best crop quality:

- ask users to bring the card as close to the camera as possible,
- keep the card fully visible,
- use a high enough `analysisTargetResolution`, and
- choose an `imageMode` that matches the intended capture experience.

---

## Image Preprocessing Modes

The `imageMode` parameter controls how the incoming camera frame is prepared before inference.

### `InputShape.FullImage`

Uses the full camera image.

- Best when the target can appear anywhere in the frame
- Preserves the full visible scene
- Uses letterboxing to fit the model input without distortion
- Lower effective resolution for the card (the card occupies fewer pixels in the model input)

### `InputShape.SquareCrop`

Crops the center of the full camera image into a square.

- Useful when the card is expected to be centered
- Can improve focus on the main subject
- Higher effective resolution for centered cards
- No coverage outside the center region
- Requires some user guidance (keep card centered)

### `InputShape.VisibleImage`

Uses only the portion of the camera feed that is actually visible in the preview.

- Prevents detections from happening outside the user-visible preview
- Letterboxes the visible region to the model input shape
- Slightly reduced coverage compared to full sensor input
- Better alignment between what the user sees and what the model analyzes
- Moderate effective resolution depending on preview crop

### `InputShape.VisibleImageSquareCrop`

Crops the center of the visible preview area into a square.

- Combines the benefits of `VisibleImage` and `SquareCrop`
- Ideal when the user is guided to place the card in the center of the screen
- Highest effective resolution for centered cards within the visible area
- Lowest coverage (center-focused only)

---

## Summary

Use `cardDetectionLite` when you need a lightweight, real-time ID card detection component in a Jetpack Compose app.

It is especially useful when you want to:

- detect cards from a live camera feed,
- wait for a stable lock-on before capturing,
- extract a cropped card image for downstream processing, and
- optionally identify subfeatures inside the detected card.

For OCR, field parsing, or document classification beyond detection, pair this module with a separate recognition pipeline.

