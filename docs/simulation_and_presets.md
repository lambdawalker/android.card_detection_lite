# Presets Catalog & Card Tracking Simulator

`CardDetectionLite` includes structured preset objects for pipeline tuning and an offline video frame simulator for testing ML models without physical hardware.

---

## 1. Presets Catalog

### 1. `CardDetectorPreset`
ML pipeline configuration for score thresholds, threading, and preprocessing:

| Property | `HighAccuracy` | `HighPerformance` (Default) | `BatterySaver` |
| :--- | :--- | :--- | :--- |
| `scoreThreshold` | `0.80f` | `0.50f` | `0.50f` |
| `iouThreshold` | `0.45f` | `0.45f` | `0.45f` |
| `lockOnThreshold` | `6` frames | `4` frames | `4` frames |
| `noDetectionCountLimit` | `10` frames | `8` frames | `6` frames |
| `inferenceIntervalMs` | `33ms` (~30 fps) | `33ms` (~30 fps) | `100ms` (~10 fps) |
| `useGpu` | `true` | `true` | `false` |
| `preProcessingImageTransformation` | `FullImage` | `SquareCrop()` | `SquareCrop()` |
| `numThreads` | `NumThreads.Default` | `NumThreads.Default` | `NumThreads.CustomCount(2)` |

### 2. `CameraPreset`
CameraX analysis resolution and focus behavior:

- **`CameraPreset.Default`**: Target resolution `2048x1080` (2K), `tapToFocusEnabled = true`, `focusOnCardEnabled = true`.
- **`CameraPreset.HighResolution`**: Target resolution `3840x2160` (4K), `tapToFocusEnabled = true`, `focusOnCardEnabled = true`.
- **`CameraPreset.FixedFocus`**: Target resolution `1920x1080` (1080p), `tapToFocusEnabled = false`, `focusOnCardEnabled = false`.

### 3. `NumThreads`
CPU thread allocation options:

- **`NumThreads.Default`**: Automatically selects half of available CPU cores (minimum 2).
- **`NumThreads.HalfCores`**: Half of available CPU cores.
- **`NumThreads.CustomCount(count)`**: Explicit thread count.

---

## 2. Card Tracking Simulator (`CardTrackingSimulator`)

`CardTrackingSimulator` enables running TFLite card detection over recorded MP4 video files (`v000.mp4`, `v001.mp4`, `v002.mp4`):

```
MP4 Video Asset ──> VideoPreviewWithFullFrameCapture ──> Bitmap Stream ──> TFLite Pipeline ──> Scoped Overlay
```

### Capabilities:

- **`VideoPreviewWithFullFrameCapture`**: ExoPlayer video preview that extracts full-resolution uncompressed `Bitmap` frames.
- **`BitmapFrameProcessor`**: Feeds extracted video bitmaps directly into `YoloDetector` and `CardDetector`.
- **`CardTrackingSimulatorViewModel`**: Orchestrates simulation state and retains `latestValidDetection`.
- Uses the same `controlOverlay: @Composable CardDetectorOverlayScope.() -> Unit` slot API as `CardDetectorLite`, allowing overlays to be tested identically against video recordings.
