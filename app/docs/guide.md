# Running and extending the sample

[Documentation index](README.md)

## Entry points

| Component | Contribution |
| --- | --- |
| CardDetectionActivity | Launcher activity: permission gate, live CardDetectorLite, model catalog, IdCaptureOverlay. |
| CardDetectionSimulationActivity | Replays raw/in_move_out through CardTrackingSimulator with customized detector thresholds. |
| MainViewModel | Receives detection/capture callbacks, manages pause/resume and navigation, supplies placeholder downstream processing. |
| ui.theme | Material colors, typography, and theme used by both activities. |

The [manifest](../src/main/AndroidManifest.xml) declares camera permission, camera hardware, and both activities. Only the live activity has the launcher intent filter.

## Build and launch

With the configured SDK/toolchain and hydrated Git LFS assets:

```bash
./gradlew :app:assembleDebug
./gradlew :app:installDebug
adb shell am start -n com.apexfission.android.cardDetectionTest/com.apexfission.android.carddetectiontest.CardDetectionActivity
adb shell am start -n com.apexfission.android.cardDetectionTest/com.apexfission.android.carddetectiontest.CardDetectionSimulationActivity
```

Installation/launch requires a connected compatible device or emulator. The simulation URI resolves a bundled raw video and does not use CameraX.

## Callback and state wiring

Activities pass function references such as `mainViewModel::onCardDetection` and `mainViewModel::onCapture`. MainViewModel does not implement special callback interfaces. It recycles rejected detections, pauses detection during accepted processing, and restores the flag in finally. Navigation is exposed as state and acknowledged after the activity handles it.

The cloud and on-device OCR functions are placeholders. Replace them with actual application processing; do not interpret sample logs as text extraction.

Before adapting its coroutine handoff into production, review [bitmap ownership](../../cardDetectionLite/docs/lifecycle.md). Cleanup inside a launched body alone does not cover cancellation before that body starts. The sample is wiring guidance, not a general asynchronous resource-management abstraction.

Detector configuration belongs in CardDetectorPreset/CameraPreset; presentation belongs in the scoped controlOverlay. See [core integration](../../cardDetectionLite/docs/integration.md) for current parameter names.
