package com.apexfission.android.permissionscompose

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.shouldShowRationale

/** Host-visible camera permission states. */
enum class CameraPermissionStatus {
    Granted,
    NotRequested,
    RationaleRequired,
    PermanentlyDenied,
}

/**
 * Observes camera permission state and exposes host-triggered actions.
 * Neither action is launched automatically by composition.
 */
@Stable
class CameraPermissionController internal constructor(
    val status: CameraPermissionStatus,
    private val request: () -> Unit,
    private val openSettings: () -> Unit,
) {
    fun requestPermission() = request()

    fun openAppSettings() = openSettings()
}

/**
 * Remembers camera permission state without requesting permission automatically.
 *
 * Android reports `shouldShowRationale == false` both before the first request and after a
 * permanent denial. This API persists whether the request has previously been launched so hosts
 * can distinguish those cases and offer an application-settings recovery route.
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun rememberCameraPermissionController(
    permissionViewModel: PermissionViewModel = viewModel(),
): CameraPermissionController {
    val context = LocalContext.current
    val applicationContext = context.applicationContext
    val requestHistory = remember(applicationContext) {
        applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    }
    var requestedBefore by rememberSaveable {
        mutableStateOf(requestHistory.getBoolean(CAMERA_REQUESTED_KEY, false))
    }
    val permissionState = permissionViewModel.rememberCameraPermissionState { granted ->
        if (granted) {
            requestedBefore = false
            requestHistory.edit().putBoolean(CAMERA_REQUESTED_KEY, false).apply()
        }
    }
    val granted = permissionState.status.isGranted

    LaunchedEffect(granted) {
        if (granted && requestedBefore) {
            requestedBefore = false
            requestHistory.edit().putBoolean(CAMERA_REQUESTED_KEY, false).apply()
        }
    }

    val status = resolveCameraPermissionStatus(
        granted = granted,
        shouldShowRationale = permissionState.status.shouldShowRationale,
        requestedBefore = requestedBefore,
    )

    return CameraPermissionController(
        status = status,
        request = {
            requestedBefore = true
            requestHistory.edit().putBoolean(CAMERA_REQUESTED_KEY, true).apply()
            permissionViewModel.launchPermissionRequest(permissionState)
        },
        openSettings = { applicationContext.openApplicationSettings() },
    )
}

internal fun resolveCameraPermissionStatus(
    granted: Boolean,
    shouldShowRationale: Boolean,
    requestedBefore: Boolean,
): CameraPermissionStatus = when {
    granted -> CameraPermissionStatus.Granted
    shouldShowRationale -> CameraPermissionStatus.RationaleRequired
    requestedBefore -> CameraPermissionStatus.PermanentlyDenied
    else -> CameraPermissionStatus.NotRequested
}

private fun Context.openApplicationSettings() {
    val intent = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", packageName, null),
    )
    if (this !is Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    startActivity(intent)
}

private const val PREFERENCES_NAME = "camera_permission_state"
private const val CAMERA_REQUESTED_KEY = Manifest.permission.CAMERA
