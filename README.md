# CardDetectionLite

`CardDetectionLite` is a real-time, GPU-accelerated Jetpack Compose module for detecting, tracking, and stabilizing ID cards (such as driver's licenses, passports, voter IDs, and national identity documents) directly from an Android camera feed or video feed.

Powered by a custom YOLO v11 TensorFlow Lite (LiteRT) model and a multi-frame dHash perceptual similarity tracking engine, it isolates target cards, stabilizes detection against frame jitter, and extracts cropped card images along with subfeatures (photos, barcodes, PDF417, MRZ text, QR codes).

`CardDetectionLite` acts as **Stage 1** of an ID-processing or identity verification pipeline—focusing purely on **detection, stabilization, and extraction** before passing high-quality crops to downstream OCR, barcode parsing, or cloud verification services.

---

## Key Capabilities

- **Jetpack Compose Native (`CardDetectorLite`)**: All-in-one composable unifying live `CameraX` feed, TFLite inference, smart auto-focus, lock-on tracking, and extensible Compose overlay UI.
- **Multi-Frame Lock-On Tracking (`CardTracker`)**: Evaluates temporal stability over consecutive frames using 64-bit perceptual difference hashing (`dHash`) and Hamming distance calculation to prevent false triggers.
- **Smart Auto-Focus Engine (`AutoFocusPolicy`)**: Decouples camera focus/exposure from Compose re-renders using spatial movement, bounding box shift, and cooldown thresholds.
- **Modular Presets**:
  - **`CardDetectorPreset`**: Tunes ML pipeline thresholds (`HighPerformance`, `HighAccuracy`, `BatterySaver`).
  - **`CameraPreset`**: Configures analysis resolution and focus behavior (`Default`, `HighResolution`, `FixedFocus`).
- **Flexible Pre-processing (`PreProcessingImageTransformation`)**: Supports `FullImage`, `SquareCrop`, `VisibleImage`, and offset crop modes to optimize model input resolution.
- **Customizable UI Slots**: Scoped slot API (`CardDetectorOverlayScope`) with built-in `IdCaptureOverlay`, animated guides, shutter controls, and flashlight toggles.
- **Offline Video Simulator (`CardTrackingSimulator`)**: Replays detection logic over video URIs (`raw/video.mp4` or file URIs) for automated UI testing and offline verification without physical hardware.
- **Multi-Module Workspace Architecture**: Clean separation between core detection, coordinate transformation chains, permissions, and model asset catalog.

---

## Workspace Modules

| Module | Type | Description |
| :--- | :--- | :--- |
| **`:cardDetectionLite`** | Android Library | Core library providing `CardDetectorLite`, CameraX preview, TFLite engine, tracking state machine, overlays, and simulator. |
| **`:tfmodel`** | Android Library | Model catalog extensions (`ModelCatalog.TfLite`), asset paths, class dictionaries, and card class ID groupings. |
| **`:permissionsCompose`** | Android Library | Jetpack Compose camera permission gatekeeper (`HandleCameraPermission`, `PermissionScreen`). |
| **`:coordinates`** | Kotlin/JVM | 2D coordinate space transformation engine (`ImageSpace`, `ImageBox`, `ImagePoint`, `ImageSpaceChain`). |
| **`:app`** | Android App | Sample test bench demonstrating live camera card detection and offline video tracking simulation. |

---

## Getting Started

### 1. Add Dependencies

#### `gradle/libs.versions.toml`
```toml
[versions]
carddetectionlite = "0.1.0-B2"
sentinel = "TFY11640F16-0.1.0-B2"
permissioncompose = "0.0.1-B0"

[libraries]
af-cdl-core = { module = "com.apexfission.android.carddetectionlite:core", version.ref = "carddetectionlite" }
af-cdl-sentinel = { module = "com.apexfission.android.carddetectionlite:sentinel-card-model", version.ref = "sentinel" }
af-permission-compose = { module = "com.apexfission.android.permissionscompose:core", version.ref = "permissioncompose" }
```

#### `app/build.gradle.kts`
```kotlin
dependencies {
    implementation(project(":cardDetectionLite"))
    implementation(project(":tfmodel"))
    implementation(project(":permissionsCompose"))
}
```

### 2. Declare Camera Permissions

In your `AndroidManifest.xml`:
```xml
<uses-permission android:name="android.permission.CAMERA" />
<uses-feature android:name="android.hardware.camera" android:required="false" />
```

---

## Quick Start Guide

### 1. ViewModel Integration (`MainViewModel`)

Manage detection pause/resume states and route card detections to your OCR or data processing pipeline:

```kotlin
class MainViewModel : ViewModel() {
    private val _isDetectionEnabled = MutableStateFlow(true)
    val isDetectionEnabled = _isDetectionEnabled.asStateFlow()

    private val _navigateBack = MutableStateFlow(false)
    val navigateBack = _navigateBack.asStateFlow()

    fun onDetection(detection: CardDetection) {
        // Handle new card detection or ongoing lock-on updates
        if (detection.lockingStatus == LockingStatus.NewCard) {
            // New card locked on! Perform haptic feedback, capture, or OCR
            processCapturedCard(detection.card)
        }
    }

    fun onCaptureRequested(detection: CardDetection) {
        processCapturedCard(detection.card)
    }

    fun onBackRequested() {
        _navigateBack.value = true
    }

    fun onBackHandled() {
        _navigateBack.value = false
    }

    private fun processCapturedCard(cardFeature: Feature) {
        viewModelScope.launch {
            _isDetectionEnabled.value = false
            try {
                // Pass cardFeature.image (Bitmap) to On-Device or Cloud OCR
            } finally {
                _isDetectionEnabled.value = true
            }
        }
    }
}
```

### 2. Compose UI Integration (`CardDetectionActivity`)

Wrap `CardDetectorLite` with `HandleCameraPermission` for seamless camera permission handling and live detection:

```kotlin
class CardDetectionActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val mainViewModel: MainViewModel by viewModels()

        setContent {
            CardDetectionTestTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->

                    HandleCameraPermission(
                        modifier = Modifier.padding(innerPadding).fillMaxSize(),
                        onBack = { finish() },
                        onNotNow = { finish() }
                    ) {
                        val isDetectionEnabled by mainViewModel.isDetectionEnabled.collectAsStateWithLifecycle()

                        CardDetectorLite(
                            modifier = Modifier.padding(innerPadding),
                            modelPath = ModelCatalog.TfLite.modelPath,
                            classLabels = ModelCatalog.TfLite.classes,
                            cardClasses = ModelCatalog.TfLite.cardClasses,
                            detectorPreset = CardDetectorPreset.HighPerformance,
                            cameraPreset = CameraPreset.Default,
                            isDetectionEnabled = isDetectionEnabled,
                            onCardDetection = mainViewModel::onDetection,
                            onBack = mainViewModel::onBackRequested,
                            onCapture = mainViewModel::onCaptureRequested,
                            controlOverlay = {
                                IdCaptureOverlay()
                            }
                        )
                    }
                }
            }
        }
    }
}
```

---

## CardDetection Result Data Structure

The `onCardDetection` callback returns a `CardDetection` object representing tracking and lock-on state:

| Property | Type | Description |
| :--- | :--- | :--- |
| `lockingStatus` | `LockingStatus` | Tracking lifecycle state (`LockingCard`, `NewCard`, `CardLocked`). |
| `id` | `Long?` | Unique ID assigned when lock-on is achieved. `null` while evaluating. |
| `lockOnProgress` | `Float` | Progress value from `0.0f` to `1.0f` towards lock-on confirmation. |
| `card` | `Feature` | Main detected ID card containing bounding box, confidence score, class ID, and cropped `Bitmap`. |
| `features` | `List<Feature>` | Sub-features detected within the card region (photos, barcodes, MRZ, QR codes). |

### `Feature` Model
```kotlin
data class Feature(
    val box: ImageBox,
    val confidence: Float,
    val classId: Int,
    val image: Bitmap
)
```

---

## Pipeline Configuration & Presets

### 1. `CardDetectorPreset` (ML Inference Options)

| Preset | Score Threshold | Lock-On Frames | Pre-processing Mode | GPU Delegate | Throttle Interval | Threads |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **`HighPerformance`** *(Default)* | `0.50f` | 4 frames | `SquareCrop()` | Enabled | 33ms (~30 FPS) | Default (3 max) |
| **`HighAccuracy`** | `0.80f` | 6 frames | `FullImage` | Enabled | 33ms (~30 FPS) | Default (3 max) |
| **`BatterySaver`** | `0.50f` | 4 frames | `SquareCrop()` | Disabled | 100ms (~10 FPS) | CustomCount(2) |

Presets can be fine-tuned via `.copy(...)` or `.change(...)`:
```kotlin
val customPreset = CardDetectorPreset.HighPerformance.copy(
    scoreThreshold = 0.60f,
    lockOnThreshold = 5,
    useGpu = true
)
```

### 2. `CameraPreset` (CameraX & Focus Options)

| Preset | Target Resolution | Tap-To-Focus | Smart Auto-Focus |
| :--- | :--- | :--- | :--- |
| **`Default`** | 2048 x 1080 (2K) | `true` | `true` |
| **`HighResolution`** | 3840 x 2160 (4K) | `true` | `true` |
| **`FixedFocus`** | 1920 x 1080 (1080p) | `false` | `false` |

### 3. Pre-processing Transformation Modes (`PreProcessingImageTransformation`)

- **`FullImage`**: Analyzes the complete camera sensor frame. Best for detecting cards anywhere in view.
- **`SquareCrop(top)` / `CenterSquareCrop`**: Center-crops the camera frame to a square. Offers higher effective card resolution when the user centers the card.
- **`VisibleImage(top)` / `CenterVisibleImage`**: Limits inference strictly to the portion of the camera feed visible on screen.
- **`VisibleImageSquareCrop(top)` / `CenterVisibleImageSquareCrop`**: Center-crops the user-visible preview region for maximum effective crop density.

### 4. CPU Threading (`NumThreads`)

Select execution thread allocation strategy:
- `NumThreads.Default` (Up to 3 cores)
- `NumThreads.Quarter`, `NumThreads.Half`, `NumThreads.ThreeQuarters`
- `NumThreads.CustomPercentage(0.6f)`
- `NumThreads.CustomCount(2)`

### 5. Quality Validators (`CardValidator`)

Filters out false candidate detections using geometric heuristics:
- `MarginValidator`: Ensures the card is not cut off by sensor borders.
- `AspectRatioValidator`: Validates aspect ratio matches target ID document standard (e.g. ID-1 ratio ~1.58).

---

## Custom UI & Overlays (`CardDetectorOverlayScope`)

Customize the camera interface via the `controlOverlay` slot using `CardDetectorOverlayScope`:

```kotlin
CardDetectorLite(
    modelPath = ModelCatalog.TfLite.modelPath,
    classLabels = ModelCatalog.TfLite.classes,
    cardClasses = ModelCatalog.TfLite.cardClasses,
    controlOverlay = {
        // Access scope variables & functions
        val detection = detectionState
        val hasValidCard = captureEnabled

        IdCaptureOverlay(
            config = IdCaptureOverlayConfig(
                title = "Document Verification",
                instructionTitle = "Scan Front of ID Card",
                instructionSubTitle = "Fit your ID card within the frame"
            )
        )
    }
)
```

### `CardDetectorOverlayScope` Properties & Methods
- **`detectionState` / `cardDetection`**: Current frame's `CardDetection` result.
- **`latestValidDetection`**: Last valid `CardDetection` preserved across temporary missed frames.
- **`imageSpaceChain`**: Transformation chain mapping image space coordinates to Compose canvas screen space.
- **`captureEnabled`**: `true` when a valid detection is ready for capture.
- **`flashlightAvailable` / `flashlightEnabled`**: Hardware flash state.
- **`capture()` / `goBack()` / `toggleFlashlight()`**: Trigger user action callbacks.

---

## Offline Video Simulation (`CardTrackingSimulator`)

Test and verify detection pipelines offline without requiring active camera hardware or physical ID cards using `CardTrackingSimulator`:

```kotlin
val videoUri = "android.resource://$packageName/raw/v002".toUri()

CardTrackingSimulator(
    videoUri = videoUri,
    modelPath = ModelCatalog.TfLite.modelPath,
    classLabels = ModelCatalog.TfLite.classes,
    cardClasses = ModelCatalog.TfLite.cardClasses,
    detectorPreset = CardDetectorPreset.BatterySaver.copy(scoreThreshold = 0.3f),
    cameraPreset = CameraPreset.Default,
    isDetectionEnabled = isDetectionEnabled,
    onCardDetection = mainViewModel::onDetection,
    onCaptureRequested = mainViewModel::onCaptureRequested,
    onBackRequested = mainViewModel::onBackRequested,
    controlOverlay = {
        IdCaptureOverlay()
    }
)
```

---

## Model Information

- **Architecture**: Lightweight YOLO v11 modified for TensorFlow Lite (LiteRT) mobile inference.
- **Target Classes**:
  - `0`: `horizontal_card`
  - `1`: `vertical_card`
  - `2`: `horizontal_card_back`
  - `3`: `photo`
  - `4`: `slim-barcode`
  - `5`: `pdf417`
  - `6`: `mrz-text`
  - `7`: `barcode`
  - `8`: `qrcode`
- **Asset Size**: ~5.3 MB
- **Dataset**: Trained on 20,000+ synthetic and real ID document variations.

---

## Output Resolution Guidelines

The cropped card image resolution returned in `CardDetection.card.image` depends on:
1. **Analysis Target Resolution** (`CameraPreset.analysisTargetResolution`): Higher sensor streams (2K or 4K) yield crisper cropped pixels.
2. **Physical Proximity**: Cards occupying a larger percentage of the camera field yield higher crop resolution.
3. **Pre-processing Strategy**: Crop-based input modes (`SquareCrop` or `VisibleImageSquareCrop`) maximize effective model input density for centered cards.

---

## Building and Running

### Build Debug APK
```bash
./gradlew :app:assembleDebug
```

### Run JVM Unit Tests
```bash
./gradlew :cardDetectionLite:testDebugUnitTest :coordinates:test
```

### Build Instrumented Android Test APK
```bash
./gradlew :cardDetectionLite:assembleDebugAndroidTest
```

---

## License

Copyright © ApexFission. All rights reserved.
