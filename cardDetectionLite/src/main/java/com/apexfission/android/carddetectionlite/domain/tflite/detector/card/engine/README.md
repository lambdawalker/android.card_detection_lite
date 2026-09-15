# `com.apexfission.android.carddetectionlite.domain.tflite.detector.card.engine`

Source set: `main` · Module: [`:cardDetectionLite`](../../../../../../../../../../../../docs/README.md)

## Contribution

Combines object detection, candidate selection, and temporal tracking behind CardDetector.

## Responsibilities and boundaries

Use buildCardDetector or buildThreadConfinedCardDetector and close the result. Tracking can reuse dHash-matched regions between YOLO passes. Bitmap inputs remain caller-owned; ImageProxy tracking recycles its temporary bitmap but leaves closing the proxy to the caller. Closing a card detector closes its underlying detector.

## Source files

- [CardDetector.kt](CardDetector.kt)
- [DefaultCardDetector.kt](DefaultCardDetector.kt)
- [ThreadConfinedCardDetector.kt](ThreadConfinedCardDetector.kt)
- [build.kt](build.kt)
