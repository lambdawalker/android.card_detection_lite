# `com.apexfission.android.carddetectionlite.domain.tflite.detector.card.selection`

Source set: `main` · Module: [`:cardDetectionLite`](../../../../../../../../../../../../docs/README.md)

## Contribution

Chooses the primary card from raw YOLO detections.

## Responsibilities and boundaries

The internal CardCandidateSelector filters configured card classes through all validators, then favors spatial overlap with previous state or confidence. It does not run inference or advance lock progression.

## Source files

- [CardCandidateSelector.kt](CardCandidateSelector.kt)
