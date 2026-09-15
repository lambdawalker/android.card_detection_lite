# Model validation

[Documentation index](README.md)

From the repository root with Android SDK and Java toolchain 17:

```bash
./gradlew :tfmodel:assembleDebug
./gradlew :cardDetectionLite:connectedDebugAndroidTest
```

The first task verifies Android library packaging. The second requires a connected device/emulator and uses the model through the core module's instrumented-test dependency.

There are no Kotlin test packages in tfmodel at this revision. Core instrumented tests exercise real model detection/tracking, output ownership, and thread affinity. Check the packaged asset path and class ordering whenever updating the model. A successful build alone does not demonstrate detection accuracy or GPU compatibility.
