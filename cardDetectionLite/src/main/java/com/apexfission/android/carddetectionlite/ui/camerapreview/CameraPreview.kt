package com.apexfission.android.carddetectionlite.ui.camerapreview

import android.util.Log
import android.util.Size
import android.view.MotionEvent
import android.view.View
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.MeteringPoint
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.apexfission.android.carddetectionlite.domain.coordinates.models.ImageSpaceChain
import com.apexfission.android.carddetectionlite.domain.tflite.model.CardDetection
import com.apexfission.android.carddetectionlite.ui.overlays.createPreviewImageSpaceChain
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay

/**
 * Encapsulates the CameraX lifecycle and provides a live camera feed.
 *
 * @param onFrame A high-frequency callback that provides frames from the camera and the corresponding [ImageSpaceChain].
 * @param onFocusEvent A callback invoked when the user taps on the preview.
 * @param lifecycleOwner The [LifecycleOwner] to which the CameraX lifecycle will be bound.
 * @param flashlightEnabled A boolean state that directly controls the camera's torch.
 * @param analysisTargetResolution The desired resolution for the image analysis stream.
 * @param focusOn When a [CardDetection] object is passed to this parameter, it triggers a smart auto-focus routine.
 * @param tapToFocusEnabled A boolean flag to enable or disable the tap-to-focus feature.
 * @param focusOnCardEnabled A boolean flag to enable or disable the smart auto-focus on card feature.
 * @param showFocusIndicator A boolean flag to enable or disable the focus indicator.
 */
@Composable
fun CameraPreview(
    onFrame: (ImageProxy, ImageSpaceChain) -> Unit,
    onFocusEvent: (CameraControl, MeteringPoint) -> Unit,
    lifecycleOwner: LifecycleOwner,
    flashlightEnabled: Boolean,
    onFlashlightAvailabilityChanged: (Boolean) -> Unit = {},
    analysisTargetResolution: Size = Size(2048, 1080),
    focusOn: CardDetection?,
    tapToFocusEnabled: Boolean = true,
    focusOnCardEnabled: Boolean = true,
    showFocusIndicator: Boolean = true,
) {
    val context = LocalContext.current
    val mainExecutor = remember(context) { ContextCompat.getMainExecutor(context) }
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }
    val onFrameState = rememberUpdatedState(onFrame)
    val previewView = remember { PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER } }

    var cameraControl by remember { mutableStateOf<CameraControl?>(null) }
    var focusPoint by remember { mutableStateOf<FocusPoint?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(modifier = Modifier.fillMaxSize(), factory = { previewView })

        if (showFocusIndicator && focusPoint != null) {
            val currentFocusPoint = focusPoint
            var isVisible by remember { mutableStateOf(false) }

            LaunchedEffect(currentFocusPoint) {
                isVisible = true
                delay(2000.milliseconds)
                isVisible = false
            }

            val alpha by animateFloatAsState(
                targetValue = if (isVisible) 1f else 0f,
                animationSpec = tween(durationMillis = 1800),
                label = "alpha"
            )

            if (currentFocusPoint != null) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val radius = 30f
                    drawCircle(
                        color = Color.White.copy(alpha = alpha),
                        radius = radius,
                        center = Offset(currentFocusPoint.x, currentFocusPoint.y),
                        style = Stroke(width = 3f)
                    )
                }
            }
        }
    }

    DisposableEffect(lifecycleOwner, flashlightEnabled, tapToFocusEnabled) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val previewUseCase = Preview.Builder().build().also {
                it.surfaceProvider = previewView.surfaceProvider
            }

            val resolutionStrategy = ResolutionStrategy(
                analysisTargetResolution, ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
            )

            val resolutionSelector = ResolutionSelector.Builder().setResolutionStrategy(resolutionStrategy).build()

            val analysisUseCaseBuilder =
                ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .setTargetRotation(previewView.display.rotation).setResolutionSelector(resolutionSelector)
                    .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)

            val analysisUseCase = analysisUseCaseBuilder.build()

            analysisUseCase.setAnalyzer(analysisExecutor) { imageProxy ->
                try {
                    val rotation = imageProxy.imageInfo.rotationDegrees
                    val uprightWidth = if (rotation % 180 == 0) imageProxy.width else imageProxy.height
                    val uprightHeight = if (rotation % 180 == 0) imageProxy.height else imageProxy.width

                    val viewWidth = previewView.width
                    val viewHeight = previewView.height

                    val spaceChain = if (viewWidth > 0 && viewHeight > 0) {
                        createPreviewImageSpaceChain(
                            videoWidth = uprightWidth,
                            videoHeight = uprightHeight,
                            viewWidth = viewWidth,
                            viewHeight = viewHeight
                        )
                    } else {
                        ImageSpaceChain.Empty
                    }

                    onFrameState.value(imageProxy, spaceChain)
                } catch (_: Throwable) {
                    imageProxy.close()
                }
            }

            try {
                cameraProvider.unbindAll()
                val camera = cameraProvider.bindToLifecycle(
                    lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, previewUseCase, analysisUseCase
                )
                cameraControl = camera.cameraControl
                val hasFlashUnit = camera.cameraInfo.hasFlashUnit()
                onFlashlightAvailabilityChanged(hasFlashUnit)
                if (hasFlashUnit) {
                    camera.cameraControl.enableTorch(flashlightEnabled)
                }
                if (tapToFocusEnabled) {
                    previewView.setOnTouchListener(fun(_: View, event: MotionEvent): Boolean {
                        focusPoint = FocusPoint(event.x, event.y)
                        onFocusEvent(
                            camera.cameraControl, previewView.meteringPointFactory.createPoint(event.x, event.y)
                        )
                        return true
                    })
                }
            } catch (e: Exception) {
                Log.e("CAM", "Camera bind failed", e)
            }
        }, mainExecutor)

        onDispose {
            onFlashlightAvailabilityChanged(false)
            runCatching { cameraProviderFuture.get().unbindAll() }
            analysisExecutor.shutdown()
        }
    }

    // --- Smart Auto-Focus Logic using AutoFocusPolicy ---
    val autoFocusPolicy = remember { AutoFocusPolicy() }

    LaunchedEffect(focusOn) {
        if (!focusOnCardEnabled) return@LaunchedEffect
        val control = cameraControl ?: return@LaunchedEffect

        val focusResult = autoFocusPolicy.shouldTriggerFocus(focusOn)
        if (focusResult.shouldFocus && focusResult.focusPoint != null) {
            val targetPoint = focusResult.focusPoint
            val viewWidth = previewView.width.toFloat()
            val viewHeight = previewView.height.toFloat()

            if (viewWidth > 0f && viewHeight > 0f) {
                focusPoint = targetPoint
                val factory = previewView.meteringPointFactory
                val point = factory.createPoint(targetPoint.x, targetPoint.y)

                val action = FocusMeteringAction.Builder(point, FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE)
                    .setAutoCancelDuration(3, TimeUnit.SECONDS)
                    .build()

                try {
                    control.startFocusAndMetering(action)
                } catch (e: Exception) {
                    Log.e("CAM", "Smart auto-focus failed", e)
                }
            }
        }
    }
}
