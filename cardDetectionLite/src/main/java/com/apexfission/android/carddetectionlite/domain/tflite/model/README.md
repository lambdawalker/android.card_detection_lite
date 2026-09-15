# `com.apexfission.android.carddetectionlite.domain.tflite.model`

Source set: `main` · Module: [`:cardDetectionLite`](../../../../../../../../../../docs/README.md)

## Contribution

Defines values shared by inference, tracking, view models, and overlays.

## Responsibilities and boundaries

Detection contains a raw class/confidence/box result; Feature describes card or feature metadata. CardDetection adds progress, nullable ID, status, and detection source. LetterboxResult carries preprocessing geometry. Callback bitmaps have a separate ownership contract.

## Source files

- [CardDetection.kt](CardDetection.kt)
- [Detection.kt](Detection.kt)
- [LetterboxResult.kt](LetterboxResult.kt)
