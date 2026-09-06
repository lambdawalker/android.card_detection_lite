# :app Module

`:app` is the sample/test bench Android application demonstrating live camera card detection and offline video tracking simulation using the `CardDetectionLite` library suite.

---

## Key Screen Activities

### 1. `CardDetectionActivity`
Demonstrates live camera feed card detection and tracking using `CardDetectorLite` and runtime camera permission management via `HandleCameraPermission`.
- **Presets**: `CardDetectorPreset.HighPerformance`, `ViewPreset.Debug`, `CameraPreset.Default`.

### 2. `CardDetectionSimulationActivity`
Demonstrates offline video processing using `CardTrackingSimulator` over pre-recorded raw video resources (`raw/sim5.mp4`). Runs real TFLite detection and tracking over simulated video frames without needing physical card samples.

---

## ViewModel Integration (`MainViewModel`)

`MainViewModel` bridges card detection events (`onDetection(CardDetection)`) to downstream OCR pipelines (on-device or cloud) while managing detection pause/resume state (`isDetectionEnabled`).

```kotlin
class MainViewModel : ViewModel() {
    private val _isDetectionEnabled = MutableStateFlow(true)
    val isDetectionEnabled = _isDetectionEnabled.asStateFlow()

    fun onDetection(card: CardDetection) {
        if (!card.isNewDetection && card.id == null) return
        if (!_isDetectionEnabled.value) return

        viewModelScope.launch {
            try {
                _isDetectionEnabled.value = false
                // Execute OCR or data extraction workflow
            } finally {
                _isDetectionEnabled.value = true
            }
        }
    }
}
```

---

## Building and Running

```bash
# Build Debug APK
./gradlew :app:assembleDebug

# Run unit and instrumented tests
./gradlew :coordinates:test :cardDetectionLite:testDebugUnitTest
```
