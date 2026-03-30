package com.apexfission.android.carddetectionlite.ui

import android.graphics.PointF
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.apexfission.android.carddetectionlite.domain.tflite.model.CardDetection
import java.util.concurrent.Executors
import kotlin.math.abs

/**
 * Encapsulates the CameraX lifecycle and provides a live camera feed.
 *
 * This component is responsible for setting up the camera, managing its lifecycle, displaying the
 * preview, and providing a stream of images for analysis. It also incorporates advanced features
 * like tap-to-focus and an intelligent auto-focus mechanism that reacts to object detection events.
 *
 * @param onFrame A high-frequency callback that provides frames from the camera for analysis.
 *                **Important:** The consumer of this callback *must* call `imageProxy.close()` on
 *                each frame to release it and allow the camera to produce the next one. Failure
 *                to do so will halt the image stream.
 * @param onFocusEvent A callback invoked when the user taps on the preview. It provides the
 *                     [CameraControl] and the tapped [MeteringPoint], allowing the caller to
 *                     initiate a focus and metering action.
 * @param lifecycleOwner The [LifecycleOwner] (typically a Composable's local lifecycle owner)
 *                       to which the CameraX lifecycle will be bound.
 * @param flashlightEnabled A boolean state that directly controls the camera's torch (flashlight).
 *                          Changes to this state will toggle the torch on or off.
 * @param analysisTargetResolution The desired resolution for the image analysis stream. Higher
 *                                 resolutions can improve detection accuracy but may impact
 *                                 performance. The specified size is a target; CameraX will
 *                                 choose the closest available resolution.
 * @param focusOn When a [CardDetection] object is passed to this parameter, it triggers a
 *                smart auto-focus and auto-exposure routine. The routine uses heuristics
 *                (size, position, cooldown) to avoid excessive focus hunting and intelligently
 *                adjusts the camera to keep the detected card sharp and well-exposed.
 * @param tapToFocusEnabled A boolean flag to enable or disable the tap-to-focus feature.
 */
@Composable
@Suppress("DEPRECATION")
fun CameraPreview(
    onFrame: (ImageProxy) -> Unit,
    onFocusEvent: (CameraControl, MeteringPoint) -> Unit,
    lifecycleOwner: LifecycleOwner,
    flashlightEnabled: Boolean,
    analysisTargetResolution: Size = Size(2048, 1080), // 2k
    focusOn: CardDetection?,
    tapToFocusEnabled: Boolean = true,
) {
    val context = LocalContext.current
    val mainExecutor = remember(context) { ContextCompat.getMainExecutor(context) }
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }
    val onFrameState = rememberUpdatedState(onFrame)
    val previewView = remember { PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER } }

    var cameraControl by remember { mutableStateOf<CameraControl?>(null) }

    AndroidView(modifier = Modifier.fillMaxSize(), factory = { previewView })

    // DisposableEffect manages the camera's setup and teardown, binding it to the lifecycle.
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

            val analysisUseCaseBuilder = ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setTargetRotation(previewView.display.rotation).setResolutionSelector(resolutionSelector)
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
                    lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, previewUseCase, analysisUseCase
                )
                cameraControl = camera.cameraControl
                camera.cameraControl.enableTorch(flashlightEnabled)
                if(tapToFocusEnabled) {
                    previewView.setOnTouchListener(fun(_: View, event: MotionEvent): Boolean {
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
            runCatching { cameraProviderFuture.get().unbindAll() }
            analysisExecutor.shutdown()
        }
    }

    // --- Smart Auto-Focus Logic ---
    var lastFocusCenter by remember { mutableStateOf<PointF?>(null) }
    var lastFocusArea by remember { mutableFloatStateOf(0f) } // Track the area
    var lastFocusTimestamp by remember { mutableLongStateOf(0L) }

    // This effect runs whenever a new `focusOn` detection is received.
    LaunchedEffect(focusOn) {
        val control = cameraControl ?: return@LaunchedEffect
        val detection = focusOn ?: return@LaunchedEffect

        val coords = detection.card.coordinates
        val detContextSize = detection.contextSize

        val currentArea = coords.width().toFloat() * coords.height()
        val totalArea = detContextSize.width().toFloat() * detContextSize.height()
        val centerX = coords.centerX().toFloat()
        val centerY = coords.centerY().toFloat()

        // Heuristic 1: Ignore very small detections to prevent focusing on noise.
        if ((currentArea / totalArea) < 0.02f) return@LaunchedEffect

        val currentTime = System.currentTimeMillis()
        val cooldownMs = 300L

        // Heuristic 2: Enforce a cooldown to prevent rapid, unnecessary focus changes.
        val isCooldownOver = (currentTime - lastFocusTimestamp) >= cooldownMs

        // Heuristic 3: Trigger focus if the card's position has shifted significantly.
        val hasMovedSignificantly = lastFocusCenter?.let { last ->
            val deltaX = abs(last.x - centerX) / detContextSize.width().toFloat()
            val deltaY = abs(last.y - centerY) / detContextSize.height().toFloat()
            deltaX > 0.05f || deltaY > 0.05f
        } ?: true // Always true for the first detection.

        // Heuristic 4: Trigger focus if the card's size has changed, indicating movement
        // towards or away from the camera.
        val hasScaleChanged = lastFocusArea.let { lastArea ->
            if (lastArea == 0f) true
            else {
                val areaChange = abs(currentArea - lastArea) / lastArea
                areaChange > 0.10f // 10% threshold
            }
        }

        // Only trigger focus if the cooldown is over AND there's a good reason to.
        if (isCooldownOver && (hasMovedSignificantly || hasScaleChanged)) {
            val factory = previewView.meteringPointFactory
            val point = factory.createPoint(centerX, centerY)

            // ADDED FLAG_AE for exposure correction
            val action = FocusMeteringAction.Builder(point, FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE)
                .setAutoCancelDuration(3, java.util.concurrent.TimeUnit.SECONDS).build()

            try {
                control.startFocusAndMetering(action)
                // Update state for the next check.
                lastFocusCenter = PointF(centerX, centerY)
                lastFocusArea = currentArea
                lastFocusTimestamp = currentTime
            } catch (e: Exception) {
                Log.e("CAM", "Smart auto-focus failed", e)
            }
        }
    }
}
