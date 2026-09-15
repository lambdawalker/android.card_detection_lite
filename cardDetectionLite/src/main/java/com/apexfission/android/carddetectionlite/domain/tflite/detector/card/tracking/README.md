# `com.apexfission.android.carddetectionlite.domain.tflite.detector.card.tracking`

Source set: `main` · Module: [`:cardDetectionLite`](../../../../../../../../../../../../docs/README.md)

## Contribution

Maintains card identity, lock progress, missing-frame recovery, and visual continuity.

## Responsibilities and boundaries

CardLockStateMachine and TemporalConsistencyChecker consume candidates and hashes. An ID is assigned at lock-on. Hash continuation contributes fractional consistency points, so lockOnThreshold is not a count of delivered callbacks.

## Source files

- [CardLockStateMachine.kt](CardLockStateMachine.kt)
- [TemporalConsistencyChecker.kt](TemporalConsistencyChecker.kt)
