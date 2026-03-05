package com.apexfission.android.carddetectionlite.domain
import androidx.camera.camera2.interop.Camera2CameraInfo
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.params.StreamConfigurationMap
import androidx.annotation.OptIn
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.CameraInfo

@OptIn(ExperimentalCamera2Interop::class)
fun isRgbaSupportedNatively(cameraInfo: CameraInfo): Boolean {
    val camera2Info = Camera2CameraInfo.from(cameraInfo)
    val map = camera2Info.getCameraCharacteristic(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)

    // Check for RGBA_8888 (which is often represented by PixelFormat.RGBA_8888)
    return map?.isOutputSupportedFor(android.graphics.PixelFormat.RGBA_8888) ?: false
}