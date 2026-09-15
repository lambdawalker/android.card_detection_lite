# `com.apexfission.android.carddetectionlite.ui.simulation`

Source set: `main` · Module: [`:cardDetectionLite`](../../../../../../../../../docs/README.md)

## Contribution

Runs card tracking on Media3/ExoPlayer video frames instead of a live camera.

## Responsibilities and boundaries

CardTrackingSimulator mirrors detector presets, overlays, and callback ownership. VideoPreviewWithFullFrameCapture and BitmapFrameProcessor capture frames. OwnedFrameBitmap guards recycling; executor dispatch transfers ownership when callback execution begins. At this revision BitmapFrameProcessor.release is a no-op, so it does not cancel already queued frame callbacks.

## Source files

- [BitmapFrameProcessor.kt](BitmapFrameProcessor.kt)
- [CardTrackingSimulator.kt](CardTrackingSimulator.kt)
- [CardTrackingSimulatorViewModel.kt](CardTrackingSimulatorViewModel.kt)
- [CardTrackingSimulatorViewModelFactory.kt](CardTrackingSimulatorViewModelFactory.kt)
- [OwnedFrameBitmap.kt](OwnedFrameBitmap.kt)
- [VideoPreviewWithFullFrameCapture.kt](VideoPreviewWithFullFrameCapture.kt)
