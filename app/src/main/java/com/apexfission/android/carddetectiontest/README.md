# `com.apexfission.android.carddetectiontest`

Source set: `main` · Module: [`:app`](../../../../../../../docs/README.md)

## Contribution

Demonstrates host integration of camera permissions, model metadata, detector UI, and capture handling.

## Responsibilities and boundaries

CardDetectionActivity is the launcher. CardDetectionSimulationActivity uses the bundled in_move_out video. MainViewModel pauses/resumes detection and accepts worker callbacks; its cloud/on-device OCR functions are placeholders.

## Source files

- [CardDetectionActivity.kt](CardDetectionActivity.kt)
- [CardDetectionSimulationActivity.kt](CardDetectionSimulationActivity.kt)
- [MainViewModel.kt](MainViewModel.kt)
