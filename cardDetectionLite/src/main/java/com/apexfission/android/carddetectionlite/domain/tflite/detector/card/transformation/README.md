# `com.apexfission.android.carddetectionlite.domain.tflite.detector.card.transformation`

Source set: `main` · Module: [`:cardDetectionLite`](../../../../../../../../../../../../docs/README.md)

## Contribution

Defines which region of an upright frame is submitted to detection.

## Responsibilities and boundaries

PreProcessingImageTransformation describes full, square, visible-preview, and visible-square crops, with centered and top-offset variants. The image package applies these strategies; this package only defines configuration.

## Source files

- [PreProcessingImageTransformation.kt](PreProcessingImageTransformation.kt)
