package com.apexfission.android.carddetectionlite.ui.permissions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi

/**
 * A "gatekeeper" Composable that manages the camera permission lifecycle for a specific feature.
 *
 * This function acts as a conditional wrapper. It checks for camera permission and decides
 * whether to display the main `content` that requires the permission, or to display the
 * `PermissionScreen` to request it from the user. It also includes a `LaunchedEffect` to
 * proactively request the permission as soon as the Composable is displayed.
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
    content: @Composable () -> Unit
) {
    val cameraPermissionState = permissionViewModel.rememberCameraPermissionState()

    // When this composable enters the composition, this effect will trigger.
    // If the permission is not already granted, it will launch the system request dialog.
    LaunchedEffect(cameraPermissionState) {
        if (!permissionViewModel.isPermissionGranted(cameraPermissionState)) {
            permissionViewModel.launchPermissionRequest(cameraPermissionState)
        }
    }

    // Conditionally display the content based on the current permission state.
    if (permissionViewModel.isPermissionGranted(cameraPermissionState)) {
        // Permission is granted, show the main feature content.
        content()
    } else {
        // Permission is not granted, show the explanatory screen.
        PermissionScreen(
            onBack = onBack,
            onAllow = { permissionViewModel.launchPermissionRequest(cameraPermissionState) },
            onNotNow = onNotNow,
            modifier = modifier
        )
    }
}
