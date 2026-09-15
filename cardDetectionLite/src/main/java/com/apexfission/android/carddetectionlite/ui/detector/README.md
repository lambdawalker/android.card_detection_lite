# `com.apexfission.android.carddetectionlite.ui.detector`

Source set: `main` · Module: [`:cardDetectionLite`](../../../../../../../../../docs/README.md)

## Contribution

Provides live-camera Compose integration and coordinates inference, state, capture, and callbacks.

## Responsibilities and boundaries

CardDetectorLite uses a nonblank stable instanceKey plus detector configuration to choose its ViewModel. SingleFlightGate prevents an inference backlog; LatestCallbackDispatcher allows one running and one pending detection callback. Worker callbacks receive caller-owned bitmaps. LatestBestDetectionStore retains an independent capture image. The default controlOverlay slot is empty.

## Source files

- [CardDetectorLite.kt](CardDetectorLite.kt)
- [CardDetectorLiteViewModel.kt](CardDetectorLiteViewModel.kt)
- [CardDetectorLiteViewModelFactory.kt](CardDetectorLiteViewModelFactory.kt)
- [CardDetectorOverlayScope.kt](CardDetectorOverlayScope.kt)
- [CardDetectorPreset.kt](CardDetectorPreset.kt)
- [DetectionFrame.kt](DetectionFrame.kt)
- [DetectorViewModelKey.kt](DetectorViewModelKey.kt)
- [LatestBestDetectionStore.kt](LatestBestDetectionStore.kt)
- [LatestCallbackDispatcher.kt](LatestCallbackDispatcher.kt)
- [NumThreads.kt](NumThreads.kt)
- [SingleFlightGate.kt](SingleFlightGate.kt)
