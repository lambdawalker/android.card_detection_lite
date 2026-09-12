package com.apexfission.android.permissionscompose

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi

/**
 * A "gatekeeper" Composable that manages the camera permission lifecycle for a specific feature.
 *
 * This function acts as a conditional wrapper. It checks for camera permission and decides
 * whether to display the main `content` that requires the permission, or to display the
 * `PermissionScreen` to request it from the user. Permission requests only begin after an
 * explicit host or user action.
 *
 * @param modifier A [Modifier] that is passed down to the `PermissionScreen` if it is displayed.
 *                 This allows for standard layout modifications from the caller.
 * @param onBack A lambda function to be invoked if the user chooses to navigate back from the
 *               `PermissionScreen`.
 * @param onNotNow A lambda function to be invoked if the user chooses the "Not Now" option on
 *                 the `PermissionScreen`.
 * @param permissionViewModel An instance of [PermissionViewModel] used to interact with the
 *                            permissions system. By default, it uses the Hilt/ViewModel mechanism
 *                            to retrieve a shared instance.
 * @param permissionContent Optional host UI for non-granted states. It receives the observable
 *                          status plus explicit request and settings actions.
 * @param content The protected Composable content that should only be displayed *after* the
 *                camera permission has been successfully granted. This is provided as a
 *                lambda, e.g., `{ MyCameraFeature() }`.
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun HandleCameraPermission(
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
    onNotNow: () -> Unit,
    permissionViewModel: PermissionViewModel = viewModel(),
    permissionContent: (@Composable (CameraPermissionController) -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val cameraPermission = rememberCameraPermissionController(permissionViewModel)

    if (cameraPermission.status == CameraPermissionStatus.Granted) {
        content()
    } else if (permissionContent != null) {
        permissionContent(cameraPermission)
    } else {
        val permanentlyDenied = cameraPermission.status == CameraPermissionStatus.PermanentlyDenied
        PermissionScreen(
            onBack = onBack,
            onAllow = if (permanentlyDenied) {
                cameraPermission::openAppSettings
            } else {
                cameraPermission::requestPermission
            },
            onNotNow = onNotNow,
            modifier = modifier,
            primaryActionText = if (permanentlyDenied) {
                stringResource(R.string.camera_permission_open_settings)
            } else {
                stringResource(R.string.camera_permission_allow)
            },
        )
    }
}
