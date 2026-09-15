# `com.apexfission.android.carddetectionlite.domain.tflite.filters`

Source set: `main` · Module: [`:cardDetectionLite`](../../../../../../../../../../docs/README.md)

## Contribution

Supplies pluggable quality rules between detection and candidate acceptance.

## Responsibilities and boundaries

All configured CardValidator instances must pass. AspectRatioValidator checks longest/shortest-side ratio; MarginValidator checks edge clearance. Custom validators should override configurationKey when equivalent instances should reuse a detector ViewModel.

## Source files

- [AspectRatioValidator.kt](AspectRatioValidator.kt)
- [CardValidator.kt](CardValidator.kt)
- [MarginValidator.kt](MarginValidator.kt)
