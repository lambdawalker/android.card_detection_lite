# :cardDetectionLite documentation

The core Android library turns live camera or recorded video frames into tracked card detections and caller-owned card images. It supplies Compose entry points, lower-level detector builders, optional OCR, and overlay components. Model assets are supplied separately.

## Read next

- [Integration and configuration](integration.md): live camera, simulation, presets, and overlays.
- [Architecture](architecture.md): pipeline boundaries, coordinates, and model contract.
- [Lifecycle and ownership](lifecycle.md): threads, frame admission, callbacks, capture, and cleanup.
- [Testing](testing.md): JVM tests, device tests, and diagnostic image extraction.
- [Package map](packages.md): every production and test package.

## Module boundary

The module depends on `:coordinates` for geometry, CameraX for capture, LiteRT/TFLite for inference, Media3 for simulation, Compose/lifecycle for UI state, and ML Kit for optional text recognition. It does not depend on `:tfmodel` in production; instrumented tests use that module's model. Permission UX is supplied by the separate `:permissionsCompose` module or by the host.

The Android library uses minSdk 26, compileSdk 36, and Java/Kotlin toolchain 17. See [build configuration](../build.gradle.kts) for declared dependencies and publication coordinates.

## Existing visual references

These assets remain in their original locations:

- [Preprocessing diagram](imageMode.svg)
- [Full image](FullImage.png)
- [Square crop](SquareCrop.png)
- [Visible image](VisibleImage.png)
- [Visible square crop](VisibleImageSquareCrop.png)
- [Demo video](demo.mp4)

Use the current transformation types and signatures in the guides/source when adapting these illustrations.
