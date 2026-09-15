# `com.apexfission.android.carddetectionlite.domain.tflite.detector.tflite.engine`

Source set: `test` · Module: [`:cardDetectionLite`](../../../../../../../../../../../../docs/README.md)

## Contribution

Checks thread confinement, shared-dispatcher composition, close races, and ownership.

## Responsibilities and boundaries

This is verification code, not a production API. Runs through the module's JVM test task. Android-facing tests use configured mocks/stubs and do not establish real-device performance.

## Source files

- [SharedEngineDispatcherTest.kt](SharedEngineDispatcherTest.kt)
- [TfliteCoreEngineTest.kt](TfliteCoreEngineTest.kt)
- [ThreadConfinedResourceTest.kt](ThreadConfinedResourceTest.kt)
