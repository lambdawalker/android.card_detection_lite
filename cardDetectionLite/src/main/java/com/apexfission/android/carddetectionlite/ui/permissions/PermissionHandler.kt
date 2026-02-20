package com.apexfission.android.carddetectionlite.ui.permissions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi

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

    LaunchedEffect(cameraPermissionState) {
        if (!permissionViewModel.isPermissionGranted(cameraPermissionState)) {
            permissionViewModel.launchPermissionRequest(cameraPermissionState)
        }
    }

    if (permissionViewModel.isPermissionGranted(cameraPermissionState)) {
        content()
    } else {
        PermissionScreen(
            onBack = onBack,
            onAllow = { permissionViewModel.launchPermissionRequest(cameraPermissionState) },
            onNotNow = onNotNow,
            modifier = modifier
        )
    }
}