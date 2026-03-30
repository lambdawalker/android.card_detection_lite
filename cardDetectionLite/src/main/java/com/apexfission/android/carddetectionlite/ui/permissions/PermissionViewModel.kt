package com.apexfission.android.carddetectionlite.ui.permissions

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

/**
 * A [ViewModel] that abstracts and simplifies interactions with the Accompanist Permissions library.
 *
 * This class serves as a thin layer to decouple the UI (Composables) from the direct implementation
 * details of the permissions library. It promotes a cleaner architecture by centralizing permission-related
 * logic, making the UI more declarative and easier to test.
 */
@OptIn(ExperimentalPermissionsApi::class)
class PermissionViewModel : ViewModel() {

    /**
     * A Composable function that creates and remembers the state for the camera permission.
     *
     * This function must be called within a Composable context (like `HandleCameraPermission`)
     * because it uses `rememberPermissionState` to hook into Compose's state management system.
     * It ensures that the `PermissionState` object persists across recompositions.
     *
     * @return The remembered [PermissionState] for `android.Manifest.permission.CAMERA`.
     */
    @Composable
    fun rememberCameraPermissionState(): PermissionState {
        return rememberPermissionState(android.Manifest.permission.CAMERA)
    }

    /**
     * Provides a synchronous check of the current permission status.
     *
     * @param permissionState The [PermissionState] object whose status needs to be checked.
     * @return `true` if the permission has been granted by the user, `false` otherwise.
     */
    fun isPermissionGranted(permissionState: PermissionState): Boolean {
        return permissionState.status.isGranted
    }

    /**
     * Triggers the asynchronous system dialog to request the permission from the user.
     *
     * This function should be called when the user explicitly agrees to grant the permission
     * (e.g., by tapping an "Allow" button). The result of this request will be reflected
     * in the `PermissionState` object, which will trigger a recomposition in the UI.
     *
     * @param permissionState The [PermissionState] for which the permission request should be launched.
     */
    fun launchPermissionRequest(permissionState: PermissionState) {
        permissionState.launchPermissionRequest()
    }
}
