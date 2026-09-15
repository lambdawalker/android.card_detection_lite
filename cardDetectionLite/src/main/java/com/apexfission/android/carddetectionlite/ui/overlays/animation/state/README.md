# `com.apexfission.android.carddetectionlite.ui.overlays.animation.state`

Source set: `main` · Module: [`:cardDetectionLite`](../../../../../../../../../../../docs/README.md)

## Contribution

Derives remembered guide transitions and animated coordinates, opacity, progress, and optional continuous effects.

## Responsibilities and boundaries

rememberAnimatedDetectionBounds combines a guide state machine and animation helpers. Frame sequences distinguish misses from recompositions. Continuous animation follows visibility/tracking policy; disabling it does not disable detection.

## Source files

- [InternalGuideState.kt](InternalGuideState.kt)
- [RememberAnimatedDetectionBounds.kt](RememberAnimatedDetectionBounds.kt)
- [guideStateMachine.kt](guideStateMachine.kt)
- [rememberAnimatedCoordinates.kt](rememberAnimatedCoordinates.kt)
- [rememberAnimatedLockOnProgress.kt](rememberAnimatedLockOnProgress.kt)
- [rememberAnimatedOpacity.kt](rememberAnimatedOpacity.kt)
- [rememberContinuousAnimations.kt](rememberContinuousAnimations.kt)
- [shouldRunContinuousAnimations.kt](shouldRunContinuousAnimations.kt)
