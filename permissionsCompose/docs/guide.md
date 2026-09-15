# Camera permission flow

[Documentation index](README.md)

`HandleCameraPermission` renders its content only after camera permission is granted. Otherwise it renders PermissionScreen or a host-provided `permissionContent` slot. Neither the gate nor `rememberCameraPermissionController` automatically opens the system permission dialog.

| Status | Interpretation | Appropriate host action |
| --- | --- | --- |
| Granted | Platform reports camera access granted. | Render protected content. |
| NotRequested | Not granted, no rationale, no recorded request. | Explain the need and offer requestPermission. |
| RationaleRequired | Android says rationale should be shown. | Explain and let the user retry. |
| PermanentlyDenied | Not granted, no rationale, request history present. | Offer openAppSettings. |

The library persists whether it launched a camera request because Android's rationale signal is false both before a first request and after permanent denial. PermanentlyDenied is an inference from those signals, not an independent system flag.

## Minimal gate

```kotlin
import androidx.compose.runtime.Composable
import com.apexfission.android.permissionscompose.HandleCameraPermission

@Composable
fun CameraGate(onBack: () -> Unit, cameraContent: @Composable () -> Unit) {
    HandleCameraPermission(
        onBack = onBack,
        onNotNow = onBack,
        content = cameraContent,
    )
}
```

For custom UI, use `permissionContent = { controller -> ... }` on the gate, or call `rememberCameraPermissionController()` directly. Read `controller.status`, and invoke `requestPermission()` or `openAppSettings()` only in response to the chosen host/user action. The default gate changes its primary action to application settings for permanent denial.

`PermissionScreen` is presentation-only: its callbacks are supplied by the caller. Customize its title, body, feature rows, and primary-action text to describe the actual host feature. `PermissionViewModel` wraps Accompanist state and request operations; it does not require Hilt.

## Host responsibilities

Declare `android.permission.CAMERA` in the application manifest, provide appropriate back/not-now navigation, and create camera content only when granted. Opening settings does not itself grant access; observe the permission state after returning. This module does not initialize CameraX, own detector instances, or request unrelated permissions.
