# Geometry tests

[Documentation index](README.md) · [Packages](packages.md)

From the repository root:

```bash
./gradlew :coordinates:test
```

JUnit tests cover models, normalized operations, parent/child mapping, and directional transformation chains. This module does not require an Android device, though the repository's overall Gradle configuration must resolve successfully. Its bytecode target is JVM 11.

For geometry changes, exercise both directions, non-unit scales, crop offsets, and boundary cases. Android preview rendering still needs verification in the core/sample application; passing these tests alone does not confirm a camera preview's rotation or fill-center setup.
