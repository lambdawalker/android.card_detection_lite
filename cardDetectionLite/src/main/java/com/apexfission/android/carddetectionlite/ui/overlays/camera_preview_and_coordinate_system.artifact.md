# Camera Preview & Coordinate System Engine

The camera preview and coordinate system bridge CameraX hardware frame streams with Jetpack Compose overlay graphics, ensuring zero-GC resource management and precise coordinate mapping.

---

## 1. CameraX Integration (`CameraPreview`)

`CameraPreview` encapsulates CameraX lifecycle binding and frame stream analysis.

### Capabilities:

1. **Frame Stream Analysis**:
   - Configures `ImageAnalysis` with `ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST` and `OUTPUT_IMAGE_FORMAT_RGBA_8888`.
   - Executes image analysis on a dedicated single-thread executor (`analysisExecutor`).
   - Automatically handles image orientation rotation (`imageInfo.rotationDegrees`).

2. **Zero-Leak Frame Management**:
   - `CardDetectorLiteViewModel.processImage` executes frame processing inside `ImageProxy.use { proxy -> ... }`.
   - Guarantees `imageProxy.close()` is invoked on every single frame—even when rate limited, disabled, or encountering exceptions—preventing CameraX buffer pool starvation.

3. **Smart Flashlight / Torch Management**:
   - Automatically probes active camera hardware capabilities via `camera.cameraInfo.hasFlashUnit()`.
   - Exposes `flashlightAvailable` state to `CardDetectorOverlayScope`.
   - Controls torch state dynamically via a dedicated `LaunchedEffect(cameraControl, flashlightEnabled)` without unbinding or re-creating the CameraX pipeline.

4. **Smart Auto-Focus Policy (`AutoFocusPolicy`)**:
   - Evaluates card detection events to trigger smart auto-focus (`CameraControl.startFocusAndMetering`).
   - Heuristics:
     - **Cooldown**: Minimum 500ms between focus actions (`cooldownMs`).
     - **Position Shift**: Triggers if card center shifts > 50px (`positionThresholdPx`).
     - **Scale Delta**: Triggers if bounding box area changes > 10% (`areaChangeThreshold`).

---

## 2. Coordinate Transformation Engine (`:coordinates` module)

The `:coordinates` module is a standalone, pure Kotlin library for mapping coordinates across nested image spaces without framework dependencies.

```
Model Space (e.g. 640x640) ──> Crop/Letterbox Space ──> Sensor Space ──> PreviewView (FILL_CENTER) ──> Screen Overlay
```

### Core Models:

- **`ImageSpace`**: Represents an 2D coordinate space defined by width and height.
- **`ImageBox`**: Immutable integer bounding box `[x, y, x2, y2, width, height]` in pixel space.
- **`NormImageBox`**: Bounding box with normalized coordinates `[0.0..1.0]`.
- **`ImagePoint` / `NormImagePoint`**: 2D point coordinates.
- **`ImageSpaceChain`**: Ordered sequence of space transformations mapping coordinates between source and target spaces.

### Coordinate Mapping Functions:

```kotlin
// Transforms a box through an ImageSpaceChain into target screen space
val screenBox = modelBox.translate(imageSpaceChain)
```

`createPreviewImageSpaceChain` calculates letterboxing, scaling, and offset parameters for `PreviewView`'s `ScaleType.FILL_CENTER` display mode, ensuring bounding boxes land exactly on physical card boundaries on screen.
