# :cardDetectionLite Module

`cardDetectionLite` is the core Android library module providing real-time, GPU-accelerated ID card detection, spatial tracking, and stabilization
directly from a live CameraX feed or video simulation.

It uses a YOLO-based TensorFlow Lite model to locate target cards, track bounding boxes across video frames, apply heuristic quality validators, and
trigger a locked-on event when a card detection stabilizes.

---

## Key Features

- **All-In-One Composable (`CardDetectorLite`)**: Integrates live `CameraX` feed, TFLite inference, lock-on stabilization, and interactive Compose
  overlays into a single component.
- **Modular Presets**:
    - **`CardDetectorPreset`**: Tunes ML pipeline parameters (`HighAccuracy`, `HighPerformance`, `BatterySaver`).
    - **`ViewPreset`**: Configures UI overlays and debug panels (`Standard`, `Minimal`, `Debug`).
    - **`CameraPreset`**: Configures CameraX resolution and focus behavior (`Default`, `HighResolution`, `FixedFocus`).
- **Smart Auto-Focus Policy (`AutoFocusPolicy`)**: Decouples CameraX auto-focus/auto-exposure decision logic from Compose UI lifecycle events based on
  cooldown, spatial movement, and bounding box area shift.
- **Multi-Frame Lock-On Tracking (`CardTracker`)**: Tracks detected cards across consecutive frames, calculates perceptual image similarity via 64-bit
  dHash (`differenceHash`), and emits stable lock-on events.
- **Heuristic Validation**: Filters false positives using `AspectRatioValidator`, `MarginValidator`, or custom functional `CardValidator`
  implementations.
- **Simulation Mode (`CardTrackingSimulator`)**: Replays detection logic over pre-recorded video URIs for offline testing and automated verification.

---

## Integration

### Gradle Dependency

```kotlin
dependencies {
    implementation(project(":cardDetectionLite"))
    implementation(project(":tfmodel")) // Provides model asset and class catalog
}
```

---

## Basic Usage

```kotlin
@Composable
fun CardDetectionScreen(mainViewModel: MainViewModel) {
    val isDetectionEnabled by mainViewModel.isDetectionEnabled.collectAsStateWithLifecycle()

    CardDetectorLite(
        instanceKey = "main-camera",
        modelPath = ModelCatalog.TfLite.modelPath,
        classLabels = ModelCatalog.TfLite.classes,
        cardClasses = ModelCatalog.TfLite.cardClasses,
        detectorPreset = CardDetectorPreset.HighPerformance,
        viewPreset = ViewPreset.Standard,
        cameraPreset = CameraPreset.Default,
        isDetectionEnabled = isDetectionEnabled,
        onCardDetection = { detection, transfer ->
            val bitmap = transfer.takeCopy()
            viewModelScope.launch(Dispatchers.Default) {
                bitmap.use { performOcr(it) }
            }
        }
    )
}
```

Detection and capture callbacks run on a library worker thread. A callback must call `takeCopy()`
synchronously if it needs the bitmap after returning. The returned bitmap belongs to the caller;
dispatch it to the appropriate application context and recycle it after its final use.

---

## Configuration Presets

### 1. `CardDetectorPreset` (ML Pipeline Options)

| Preset                | Score Threshold | Lock-On Frames | Input Preprocessing     | GPU Delegate       | Throttle Interval |
|:----------------------|:----------------|:---------------|:------------------------|:-------------------|:------------------|
| **`HighAccuracy`**    | `0.35f`         | 6 frames       | `InputShape.FullImage`  | Enabled (`true`)   | 33ms (30 fps~)    |
| **`HighPerformance`** | `0.65f`         | 4 frames       | `InputShape.SquareCrop` | Enabled (`true`)   | 33ms (30 fps~)    |
| **`BatterySaver`**    | `0.50f`         | 4 frames       | `InputShape.SquareCrop` | Disabled (`false`) | 100ms (10 fps~)   |

### 2. `ViewPreset` (UI Overlay Options)

| Preset         | Bounding Boxes | Class Names | Flashlight Button | Lock-On Overlay | Debug Panel |
|:---------------|:---------------|:------------|:------------------|:----------------|:------------|
| **`Standard`** | `false`        | `false`     | `true`            | `true`          | `false`     |
| **`Minimal`**  | `false`        | `false`     | `false`           | `true`          | `false`     |
| **`Debug`**    | `true`         | `true`      | `true`            | `true`          | `true`      |

### 3. `CameraPreset` (CameraX Options)

| Preset               | Target Resolution   | Tap-To-Focus | Smart Auto-Focus |
|:---------------------|:--------------------|:-------------|:-----------------|
| **`Default`**        | 2048 x 1080 (2K)    | `true`       | `true`           |
| **`HighResolution`** | 3840 x 2160 (4K)    | `true`       | `true`           |
| **`FixedFocus`**     | 1920 x 1080 (1080p) | `false`      | `false`          |

---

## Architecture & Package Overview

- **`com.apexfission.android.carddetectionlite.ui`**:
    - `CardDetectorLite`: Main camera detection Composable.
    - `CardDetectorPreset`, `ViewPreset`, `CameraPreset`: Configuration presets.
    - `CardDetectorLiteViewModel` & `CardDetectorLiteViewModelFactory`: StateFlow management and frame throttling.
- **`com.apexfission.android.carddetectionlite.ui.camerapreview`**:
    - `CameraPreview`: CameraX lifecycle binding and frame analyzer integration.
    - `AutoFocusPolicy`: Smart auto-focus decision state machine.
    - `DetectionOverlay`, `CardLockOnOverlay`, `DebugOverlay`: Render overlays.
- **`com.apexfission.android.carddetectionlite.ui.simulation`**:
    - `CardTrackingSimulator`: Video URI simulation preview.
- **`com.apexfission.android.carddetectionlite.domain.tflite.detector`**:
    - `CardTracker`: Multi-frame detection stabilization and lock-on state machine.
    - `YoloDetector` & `YoloPostProcessor`: TFLite tensor execution and NMS candidate filtering.
- **`com.apexfission.android.carddetectionlite.domain.tflite.filters`**:
    - `CardValidator`, `AspectRatioValidator`, `MarginValidator`: Quality filtering.

---

## Testing

```bash
# Run local JVM unit tests
./gradlew :cardDetectionLite:testDebugUnitTest

# Build instrumented Android test APK
./gradlew :cardDetectionLite:assembleDebugAndroidTest
```
