# `com.apexfission.android.carddetectionlite.domain.tflite.image`

Source set: `main` · Module: [`:cardDetectionLite`](../../../../../../../../../../docs/README.md)

## Contribution

Implements upright frame conversion, preprocessing crops, reusable letterboxing, and perceptual hashing.

## Responsibilities and boundaries

cropWithOffset returns offsets for restoring source coordinates. LetterboxBuilder owns reusable output and releases it on close; do not treat it as an independent retained image. dHash/Hamming helpers establish visual continuity, not identity verification.

## Source files

- [ImageOperations.kt](ImageOperations.kt)
- [ImageProxyExt.kt](ImageProxyExt.kt)
- [LetterboxBuilder.kt](LetterboxBuilder.kt)
- [differenceHash.kt](differenceHash.kt)
