# `com.apexfission.android.permissionscompose`

Source set: `main` · Module: [`:permissionsCompose`](../../../../../../../docs/README.md)

## Contribution

Provides a host-driven camera permission gate with contextual UI and settings recovery.

## Responsibilities and boundaries

rememberCameraPermissionController exposes status plus explicit request/settings actions; composition never requests permission. HandleCameraPermission renders granted content or default/custom permission UI. Permanent denial is inferred from rationale and persisted request history, not a distinct platform flag.

## Source files

- [CameraPermissionState.kt](CameraPermissionState.kt)
- [PermissionHandler.kt](PermissionHandler.kt)
- [PermissionScreen.kt](PermissionScreen.kt)
- [PermissionViewModel.kt](PermissionViewModel.kt)
