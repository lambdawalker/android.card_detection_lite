# `com.apexfission.android.carddetectionlite.resource`

Source set: `main` · Module: [`:cardDetectionLite`](../../../../../../../../docs/README.md)

## Contribution

Provides scoped bitmap cleanup for consumers that complete processing inside a block.

## Responsibilities and boundaries

Bitmap.use recycles in finally, including on exceptions. It is not reference counting: launching asynchronous work then returning recycles too early. An asynchronous owner must guarantee eventual cleanup, including cancellation before its work starts.

## Source files

- [BitmapUse.kt](BitmapUse.kt)
