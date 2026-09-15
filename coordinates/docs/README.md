# :coordinates documentation

This Kotlin/JVM library supplies the geometry used to connect image preprocessing, detections, and preview drawing. It has no CameraX, bitmap, or TFLite runtime dependency. Compose runtime is compile-only for annotations; bytecode targets JVM 11.

- [Geometry guide](guide.md): primitives, directional chains, and an example.
- [Testing](testing.md): local geometry verification.
- [Package map](packages.md): production and test responsibilities.

The core module depends on these types. See [build configuration](../build.gradle.kts) for the module boundary.
