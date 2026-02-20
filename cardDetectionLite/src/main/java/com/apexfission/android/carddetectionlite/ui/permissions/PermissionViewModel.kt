package com.apexfission.android.carddetectionlite.ui.permissions

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

@OptIn(ExperimentalPermissionsApi::class)
class PermissionViewModel : ViewModel() {

    @Composable
    fun rememberCameraPermissionState(): PermissionState {
        return rememberPermissionState(android.Manifest.permission.CAMERA)
    }

    fun isPermissionGranted(permissionState: PermissionState): Boolean {
        return permissionState.status.isGranted
    }

    fun launchPermissionRequest(permissionState: PermissionState) {
        permissionState.launchPermissionRequest()
    }
}