# `com.apexfission.android.carddetectionlite.ui.overlays`

Source set: `main` · Module: [`:cardDetectionLite`](../../../../../../../../../docs/README.md)

## Contribution

Provides detection, lock-on, debug, and ID-capture UI over scoped detector state.

## Responsibilities and boundaries

IdCaptureOverlay renders guidance and capture/back/torch actions. IdCaptureOverlayConfig controls presentation. Forwarders and type aliases preserve animated APIs implemented in the animation subpackage. Overlays do not own inference or delivered images.

## Source files

- [AnimatedDetectionBounds.kt](AnimatedDetectionBounds.kt)
- [AnimatedDetectionCanvas.kt](AnimatedDetectionCanvas.kt)
- [CardLockOnOverlay.kt](CardLockOnOverlay.kt)
- [DebugOverlay.kt](DebugOverlay.kt)
- [DetectionOverlay.kt](DetectionOverlay.kt)
- [IdCaptureOverlay.kt](IdCaptureOverlay.kt)
- [IdCaptureOverlayConfig.kt](IdCaptureOverlayConfig.kt)
