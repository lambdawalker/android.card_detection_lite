# `com.apexfission.android.carddetectionlite.domain.tflite.detector.card.engine`

Source set: `test` · Module: [`:cardDetectionLite`](../../../../../../../../../../../../docs/README.md)

## Contribution

Checks serialized tracking, dedicated-thread use, dHash continuation, and YOLO fallback.

## Responsibilities and boundaries

This is verification code, not a production API. Runs through the module's JVM test task. Android-facing tests use configured mocks/stubs and do not establish real-device performance.

## Source files

- [CardDetectorConcurrencyTest.kt](CardDetectorConcurrencyTest.kt)
- [CardDetectorThreadTest.kt](CardDetectorThreadTest.kt)
- [DefaultCardDetectorTest.kt](DefaultCardDetectorTest.kt)
