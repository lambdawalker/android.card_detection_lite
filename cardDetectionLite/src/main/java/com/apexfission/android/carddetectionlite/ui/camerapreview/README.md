# `com.apexfission.android.carddetectionlite.ui.camerapreview`

Source set: `main` · Module: [`:cardDetectionLite`](../../../../../../../../../docs/README.md)

## Contribution

Connects CameraX preview and analysis to Compose lifecycle, focus, and torch controls.

## Responsibilities and boundaries

CameraPreview delivers ImageProxy frames and a coordinate chain; consumers must close delivered proxies. CameraProviderSession coordinates binding/disposal races. AutoFocusPolicy limits repeated requests. Utilities map through fill-center preview cropping. CameraPreset requests a resolution but does not guarantee hardware output dimensions.

## Source files

- [AutoFocusPolicy.kt](AutoFocusPolicy.kt)
- [CameraPreset.kt](CameraPreset.kt)
- [CameraPreview.kt](CameraPreview.kt)
- [CameraProviderSession.kt](CameraProviderSession.kt)
- [utils.kt](utils.kt)
