# Sample verification

[Documentation index](README.md)

```bash
./gradlew :app:assembleDebug
./gradlew :app:connectedDebugAndroidTest
```

The instrumented suite currently provides an application-context smoke test, not full end-to-end detection coverage. Device/emulator execution and Android SDK/toolchain setup are required.

Manually exercise live permission request/recovery, camera preview, focus/torch on supported hardware, overlay capture/back actions, and navigation. Launch the simulation activity to inspect tracking against the bundled video. Detection logic and model tests live in the [core module](../../cardDetectionLite/docs/testing.md), with geometry tests in [coordinates](../../coordinates/docs/testing.md).
