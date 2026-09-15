# Permission tests

[Documentation index](README.md) · [Packages](packages.md)

```bash
./gradlew :permissionsCompose:testDebugUnitTest
```

Run from the repository root with the configured Android SDK and Java toolchain 17. CameraPermissionStatusTest covers grant precedence, first request, rationale after denial, and denial without rationale after a recorded request.

On a device, verify fresh-install composition does not launch a dialog, explicit requests do, denial permits the appropriate recovery, and returning from settings updates the gate. The current local suite tests status resolution; it does not drive Android dialogs or settings screens.
