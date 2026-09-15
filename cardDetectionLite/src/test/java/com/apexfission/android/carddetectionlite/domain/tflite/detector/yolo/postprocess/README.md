# `com.apexfission.android.carddetectionlite.domain.tflite.detector.yolo.postprocess`

Source set: `test` · Module: [`:cardDetectionLite`](../../../../../../../../../../../../docs/README.md)

## Contribution

Checks IoU, class-aware NMS, and invalid/out-of-bounds prediction handling.

## Responsibilities and boundaries

This is verification code, not a production API. Runs through the module's JVM test task. Android-facing tests use configured mocks/stubs and do not establish real-device performance.

## Source files

- [IntersectionOverUnionTest.kt](IntersectionOverUnionTest.kt)
- [YoloNmsTest.kt](YoloNmsTest.kt)
- [YoloPostProcessorCoordinateSafetyTest.kt](YoloPostProcessorCoordinateSafetyTest.kt)
