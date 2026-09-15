# `com.apexfission.android.carddetectionlite.domain.tflite.image`

Source set: `test` · Module: [`:cardDetectionLite`](../../../../../../../../../../docs/README.md)

## Contribution

Checks crop strategies, hash comparison, letterbox ownership, and serialized preprocessing.

## Responsibilities and boundaries

This is verification code, not a production API. Runs through the module's JVM test task. Android-facing tests use configured mocks/stubs and do not establish real-device performance.

## Source files

- [HammingDistanceAndVisualSimilarityTest.kt](HammingDistanceAndVisualSimilarityTest.kt)
- [ImageOperationsTest.kt](ImageOperationsTest.kt)
- [LetterboxBuilderTest.kt](LetterboxBuilderTest.kt)
