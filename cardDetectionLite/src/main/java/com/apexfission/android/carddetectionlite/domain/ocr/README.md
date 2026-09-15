# `com.apexfission.android.carddetectionlite.domain.ocr`

Source set: `main` · Module: [`:cardDetectionLite`](../../../../../../../../../docs/README.md)

## Contribution

Adds optional on-device Latin text recognition after capture.

## Responsibilities and boundaries

OcrWrapper.run suspends and returns OcrResult text blocks with nullable bounding boxes. Input bitmaps remain caller-owned. close rejects new requests and defers recognizer disposal until active wrapper requests finish. Detection does not automatically invoke OCR.

## Source files

- [OcrWrapper.kt](OcrWrapper.kt)
