# `com.apexfission.android.carddetectionlite.ui.simulation`

Source set: `test` · Module: [`:cardDetectionLite`](../../../../../../../../../docs/README.md)

## Contribution

Checks exactly-once recycling and bitmap ownership in accepted/rejected executor dispatch.

## Responsibilities and boundaries

This is verification code, not a production API. Runs through the module's JVM test task. Android-facing tests use configured mocks/stubs and do not establish real-device performance.

## Source files

- [BitmapCallbackDispatchTest.kt](BitmapCallbackDispatchTest.kt)
- [OwnedFrameBitmapTest.kt](OwnedFrameBitmapTest.kt)
