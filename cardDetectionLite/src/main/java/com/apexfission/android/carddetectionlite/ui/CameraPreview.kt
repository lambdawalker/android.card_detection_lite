package com.apexfission.android.carddetectionlite.ui

import android.util.Log
import android.util.Size
import android.view.MotionEvent
import android.view.View
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.MeteringPoint
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.util.concurrent.Executors

@Composable
@Suppress("DEPRECATION")
fun CameraPreview(
    onFrame: (ImageProxy) -> Unit,
    onFocusEvent: (CameraControl, MeteringPoint) -> Unit,
    lifecycleOwner: LifecycleOwner,
    flashlightEnabled: Boolean,
    analysisTargetResolution: Size = Size(1920, 1080)
) {
    val context = LocalContext.current
    val mainExecutor = remember(context) { ContextCompat.getMainExecutor(context) }
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }
    val onFrameState = rememberUpdatedState(onFrame)
    val previewView = remember { PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER } }

    AndroidView(modifier = Modifier.fillMaxSize(), factory = { previewView })

    DisposableEffect(lifecycleOwner, flashlightEnabled) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val previewUseCase = Preview.Builder()
                .build()
                .also {
                    it.surfaceProvider = previewView.surfaceProvider
                }

            val resolutionStrategy = ResolutionStrategy(
                analysisTargetResolution,
                ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
            )

            val resolutionSelector = ResolutionSelector.Builder()
                .setResolutionStrategy(resolutionStrategy)
                .build()

            val analysisUseCaseBuilder = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setTargetRotation(previewView.display.rotation)
                .setResolutionSelector(resolutionSelector)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)


            val analysisUseCase = analysisUseCaseBuilder.build()

            analysisUseCase.setAnalyzer(analysisExecutor) { imageProxy ->
                try {
                    onFrameState.value(imageProxy)
                } catch (_: Throwable) {
                    imageProxy.close()
                }
            }

            try {
                cameraProvider.unbindAll()
                val camera = cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    previewUseCase,
                    analysisUseCase
                )
                camera.cameraControl.enableTorch(flashlightEnabled)
                previewView.setOnTouchListener(fun(_: View, event: MotionEvent): Boolean {
                    onFocusEvent(camera.cameraControl, previewView.meteringPointFactory.createPoint(event.x, event.y))
                    return true
                })
            } catch (e: Exception) {
                Log.e("CAM", "Bind failed", e)
            }
        }, mainExecutor)

        onDispose {
            runCatching { cameraProviderFuture.get().unbindAll() }
            analysisExecutor.shutdown()
        }
    }
}