# UI Overlays & Scoped Slot Architecture

`CardDetectionLite` provides an extensible, scoped Compose UI overlay architecture designed for standard ID verification flows and custom developer UIs.

---

## 1. Scoped Overlay Slot API (`CardDetectorOverlayScope`)

`CardDetectorLite` exposes a developer-customizable Compose slot API via `controlOverlay`:

```kotlin
@Composable
fun CardDetectorLite(
    ...,
    controlOverlay: @Composable CardDetectorOverlayScope.() -> Unit = {}
)
```

### Scope Interface (`CardDetectorOverlayScope`):

```kotlin
interface CardDetectorOverlayScope {
    val detectionState: CardDetection?         // Active frame detection
    val cardDetection: CardDetection?           // Alias for detectionState
    val latestBestDetection: CardDetection?     // Highest-confidence detection retained for capture
    val imageSpaceChain: ImageSpaceChain?       // Screen coordinate mapping chain
    val captureEnabled: Boolean                // True when latestBestDetection != null
    val flashlightAvailable: Boolean           // True if camera supports flash
    val flashlightEnabled: Boolean             // Current torch state
    val detectorPreset: CardDetectorPreset     // Pipeline ML preset
    val cameraPreset: CameraPreset             // Camera preset

    fun capture()                              // Triggers capture action
    fun goBack()                               // Triggers back navigation action
    fun toggleFlashlight()                     // Toggles camera torch
}
```

This decoupled slot design allows developers to write custom Compose UI overlays without `CardDetectorLite` needing to know custom overlay configuration types or base classes.

---

## 2. Shared Canvas & Bounds Engine (`AnimatedDetectionCanvas`)

`AnimatedDetectionCanvas` abstracts coordinate transformation, smooth bounds interpolation, opacity fading, and lock-on progress for overlays.

```kotlin
controlOverlay = {
    AnimatedDetectionCanvas {
        if (bounds.opacity > 0f) {
            drawRoundRect(
                color = Color.Cyan.copy(alpha = bounds.opacity),
                topLeft = bounds.topLeft,
                size = bounds.size,
                cornerRadius = CornerRadius(16.dp.toPx()),
                style = Stroke(width = 3.dp.toPx())
            )
        }
    }
}
```

### Shared Animation Capabilities (`rememberAnimatedDetectionBounds`):

- **Coordinates**: `bounds.left`, `top`, `right`, `bottom`, `width`, `height`, `center`, `topLeft`, `size`, `rectF`.
- **Lock-On Progress**: `bounds.lockOnProgress` (raw) and `bounds.smoothProgress` (spring-animated `0.0..1.0`).
- **Pulse & Sweep**: `bounds.breathe` (infinite pulse `0.96f..1.04f`) and `bounds.sweepPhase` (continuous phase `0.0..1.0`).
- **Opacity**: `bounds.opacity` (smooth fade in/out).
- **Reset Configuration (`DetectionAnimationConfig`)**:
  - `idleOpacity` (default 0.35f).
  - `detectedOpacity` (default 0.85f).
  - `missingCardResetDelayMs` (default 2,000ms).
  - `resetPositionOnMissing` toggle: `true` (animates back to center for ID capture overlays) or `false` (retains last detected bounds for lock-on tracking overlays).

---

## 3. Built-In Overlays Catalog

### 1. `IdCaptureOverlay` (`IdCaptureOverlayConfig`)
Production ID verification overlay following the standard layout:
- **Top Bar**: Back button (`goBack()`), title ("Verify Your Identity").
- **Instructions**: Header ("Position your ID within the frame") and subtitle.
- **Card Guide**: Dashed boundary line with L-bracket corner accents, tracking detected card bounds at 85% opacity, fading on loss, and returning to center after 2s.
- **Controls**:
  - Centered shutter/capture button: Bouncy spring scale transition when enabled, staying enabled once the first valid detection is stored.
  - Flashlight toggle button: Bottom-right circular button with torch state tinting.

### 2. `CardLockOnOverlay`
Glowing progress frame around detected cards:
- Color transitions from white to cyan to green as `lockOnProgress` advances.
- Corner bracket thickness and tick marks contract dynamically as card locks in.
- Animated sweep highlight line across top border upon lock-in completion.
- Uses `resetPositionOnMissing = false` to retain position on lost frames while fading out.

### 3. `DetectionOverlay`
Diagnostic bounding box viewer:
- Renders bounding rectangles for card features and primary targets.
- Displays class labels, confidence percentages, and frame miss metrics.

### 4. `DebugOverlay`
Performance diagnostic panel anchored to the bottom of the screen:
- Displays runtime parameters: GPU delegate status, CPU threads, inference interval, lock-on threshold, input mode, and dHash limits.

---

## 4. Overlay Drawing Library (`ui.overlays.draw`)

Modular drawing utilities for custom canvas overlays:

- **`PathUtils.kt`**: `drawBlurredPath`, `drawGlowPath`, `buildRoundedPolylinePath`, `lerpF`.
- **`SegmentBuilders.kt`**: `buildCorner`, `buildCorners`, `buildConnector`, `buildConnectors`.
- **`RoundedSegment.kt`**: `RoundedSegment` point container.
