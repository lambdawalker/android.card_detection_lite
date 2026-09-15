# Integration and configuration

[Documentation index](README.md)

## Live camera

Add `:cardDetectionLite` and `:tfmodel` to the host application's dependencies. Add `:permissionsCompose` if using its gate. Declare `android.permission.CAMERA` in the host manifest and obtain runtime permission before rendering the camera.

This minimal screen uses the current function callbacks and explicitly releases images it does not consume:

```kotlin
import androidx.compose.runtime.Composable
import com.apexfission.android.carddetectionlite.domain.ModelCatalog
import com.apexfission.android.carddetectionlite.tfmodel.cardClasses
import com.apexfission.android.carddetectionlite.tfmodel.classes
import com.apexfission.android.carddetectionlite.tfmodel.modelPath
import com.apexfission.android.carddetectionlite.ui.camerapreview.CameraPreset
import com.apexfission.android.carddetectionlite.ui.detector.CardDetectorLite
import com.apexfission.android.carddetectionlite.ui.overlays.IdCaptureOverlay
import com.apexfission.android.permissionscompose.HandleCameraPermission

@Composable
fun CardScreen(onBack: () -> Unit) {
    HandleCameraPermission(onBack = onBack, onNotNow = onBack) {
        CardDetectorLite(
            instanceKey = "identity-camera",
            modelPath = ModelCatalog.TfLite.modelPath,
            classLabels = ModelCatalog.TfLite.classes,
            cardClasses = ModelCatalog.TfLite.cardClasses,
            cameraPreset = CameraPreset.Default,
            onCardDetection = { _, bitmap -> bitmap.recycle() },
            onCapture = { _, bitmap -> bitmap.recycle() },
            onBack = onBack,
            controlOverlay = { IdCaptureOverlay() },
        )
    }
}
```

Replace the recycling callbacks with application processing that guarantees cleanup. Both detection and capture callbacks run on a worker thread. See [ownership](lifecycle.md) before dispatching images asynchronously.

Give sibling components different, stable, nonblank `instanceKey` values. Detector configuration contributes to the ViewModel key; callbacks and camera-only configuration do not. The model catalog properties are extensions and require imports from `tfmodel`.

## Detector presets

Values below follow the preset initializers, including where older source comments describe different values.

| Preset | Score threshold | Lock consistency points | Missing-frame limit | Minimum interval | GPU requested | Preprocessing |
| --- | --- | --- | --- | --- | --- | --- |
| HighAccuracy | 0.80 | 7 | 6 | 33 ms | Yes | FullImage |
| HighPerformance | 0.50 | 5 | 8 | 33 ms | Yes | SquareCrop() |
| BatterySaver | 0.50 | 4 | 10 | 100 ms | No | SquareCrop() |

All use IoU 0.45 by default. BatterySaver uses `NumThreads.CustomCount(2)`; the other presets use `NumThreads.Default`. These intervals are admission limits, not promised frame rates. Hash continuation contributes fractional lock points.

Use `CardDetectorPreset.HighPerformance.copy(scoreThreshold = 0.6f)` to override a value. `isDetectionEnabled` controls processing independently. The default validators are `MarginValidator` and `AspectRatioValidator`; custom rules implement `CardValidator`.

## Camera and preprocessing

| CameraPreset | Analysis target | Tap focus | Card focus |
| --- | --- | --- | --- |
| Default | 2048 × 1080 | Yes | Yes |
| HighResolution | 3840 × 2160 | Yes | Yes |
| FixedFocus | 2048 × 1080 | No | No |

The live composable currently defaults to `CameraPreset.HighResolution`. Requested sizes are subject to CameraX/device selection.

`PreProcessingImageTransformation` offers `FullImage`, `CenterSquareCrop`, `SquareCrop(top)`, `CenterVisibleImage`, `VisibleImage(top)`, `CenterVisibleImageSquareCrop`, and `VisibleImageSquareCrop(top)`. Centered variants and top-offset variants are distinct. Cropping changes the inference region; it does not change model tensor dimensions.

## Overlays and capture

Supply `controlOverlay` with `IdCaptureOverlay()`, `CardLockOnOverlay()`, `DetectionOverlay()`, `DebugOverlay()`, or custom scoped UI. The default slot is empty. There is no current `ViewPreset` parameter.

`CardDetectorOverlayScope` exposes detection state, retained best detection, coordinate mapping, presets, labels, flashlight state, and actions. Customize the built-in guide with `IdCaptureOverlayConfig`, or use the animation package's canvas/bounds for custom drawing.

Capture uses the retained best image and sends an independent bitmap copy. It does not request a fresh camera still. See [capture semantics](lifecycle.md).

## Recorded video

Use `CardTrackingSimulator` from `ui.simulation`, passing `videoUri`, model metadata, presets, and a unique `instanceKey`. Its action parameters are `onCaptureRequested` and `onBackRequested`; live camera uses `onCapture` and `onBack`. Both use a two-argument `onCardDetection` callback and a scoped `controlOverlay`.

The [sample simulation activity](../../app/src/main/java/com/apexfission/android/carddetectiontest/CardDetectionSimulationActivity.kt) is a complete wiring example. Video simulation does not need camera permission, but the supplied URI must be readable by the app.
