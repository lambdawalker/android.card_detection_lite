# :permissionsCompose Module

`permissionsCompose` is a lightweight Jetpack Compose library module providing a camera runtime permission request workflow wrapper (`HandleCameraPermission`).

Built on top of Google Accompanist Permissions, it abstracts runtime permission checks, rationale dialogs, and permission denied state handlers into a clean Compose composable wrapper.

---

## Key Features

- **`HandleCameraPermission`**: Top-level composable wrapper that manages `Manifest.permission.CAMERA` state.
- **Host-driven request**: Merely composing the permission gate never launches the Android dialog.
- **Explicit states**: Distinguishes first request, rationale, permanent denial, and grant.
- **Settings recovery**: Exposes an action that opens this application's system settings.
- **Graceful Fallbacks**:
  - `onBack`: Triggered when the user presses back or navigates away.
  - `onNotNow`: Triggered when permission is declined.
- **Automatic Continuation**: Renders target content immediately when permission is granted.

---

## Integration

### `build.gradle.kts`
```kotlin
dependencies {
    implementation(project(":permissionsCompose"))
}
```

For fully custom contextual UI, observe the controller directly:

```kotlin
@Composable
fun CameraPermissionGate() {
    val cameraPermission = rememberCameraPermissionController()

    when (cameraPermission.status) {
        CameraPermissionStatus.Granted -> CameraContent()
        CameraPermissionStatus.NotRequested,
        CameraPermissionStatus.RationaleRequired -> PermissionExplanation(
            onContinue = cameraPermission::requestPermission,
        )
        CameraPermissionStatus.PermanentlyDenied -> PermissionSettingsRecovery(
            onOpenSettings = cameraPermission::openAppSettings,
        )
    }
}
```

The request and settings actions only run when the host invokes them. `PermanentlyDenied` is
derived from Android's rationale signal plus a persisted record that this library previously
launched the camera request.

---

## Usage Example

```kotlin
@Composable
fun MainScreen(onFinish: () -> Unit) {
    HandleCameraPermission(
        modifier = Modifier.fillMaxSize(),
        onBack = onFinish,
        onNotNow = onFinish
    ) {
        // Permission granted: Render camera feed composable
        CardDetectorLite(
            instanceKey = "main-camera",
            modelPath = ModelCatalog.TfLite.modelPath,
            classLabels = ModelCatalog.TfLite.classes,
            cardClasses = ModelCatalog.TfLite.cardClasses,
            isDetectionEnabled = true,
            onCardDetection = { card -> /* Handle detection */ }
        )
    }
}
```
