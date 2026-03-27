package com.apexfission.android.carddetectionlite.ui

import android.graphics.PointF
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
import com.apexfission.android.carddetectionlite.domain.tflite.model.CardDetection
import java.util.concurrent.Executors
import androidx.camera.core.FocusMeteringAction
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.math.abs
import kotlinx.coroutines.delay

@Composable
@Suppress("DEPRECATION")
fun CameraPreview(
    onFrame: (ImageProxy) -> Unit,
    onFocusEvent: (CameraControl, MeteringPoint) -> Unit,
    lifecycleOwner: LifecycleOwner,
    flashlightEnabled: Boolean,
    analysisTargetResolution: Size = Size(1920, 1080),
    focusOn: CardDetection?
) {
    val context = LocalContext.current
    val mainExecutor = remember(context) { ContextCompat.getMainExecutor(context) }
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }
    val onFrameState = rememberUpdatedState(onFrame)
    val previewView = remember { PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER } }


    var cameraControl by remember { mutableStateOf<CameraControl?>(null) }

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
                cameraControl = camera.cameraControl
                camera.cameraControl.enableTorch(flashlightEnabled)
                previewView.setOnTouchListener(fun(_: View, event: MotionEvent): Boolean {
                    onFocusEvent(
                        camera.cameraControl,
                        previewView.meteringPointFactory.createPoint(event.x, event.y)
                    )
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

    var lastFocusCenter by remember { mutableStateOf<PointF?>(null) }
    var lastFocusArea by remember { mutableFloatStateOf(0f) } // Track the area
    var lastFocusTimestamp by remember { mutableLongStateOf(0L) }

    LaunchedEffect(focusOn) {
        val control = cameraControl ?: return@LaunchedEffect
        val detection = focusOn ?: return@LaunchedEffect

        val coords = detection.card.coordinates
        val context = detection.contextSize

        // 1. Current Area and Center
        val currentArea = coords.width().toFloat() * coords.height().toFloat()
        val totalArea = context.width().toFloat() * context.height().toFloat()
        val centerX = coords.centerX().toFloat()
        val centerY = coords.centerY().toFloat()

        // 2. Minimum Size Check (Card must be at least 2% of frame)
        if ((currentArea / totalArea) < 0.02f) return@LaunchedEffect

        val currentTime = System.currentTimeMillis()
        val cooldownMs = 300L

        // 3. Cooldown Check
        val isCooldownOver = (currentTime - lastFocusTimestamp) >= cooldownMs

        // 4. Movement Check (Normalized to 5% of screen)
        val hasMovedSignificantly = lastFocusCenter?.let { last ->
            val deltaX = abs(last.x - centerX) / context.width().toFloat()
            val deltaY = abs(last.y - centerY) / context.height().toFloat()
            deltaX > 0.05f || deltaY > 0.05f
        } ?: true

        // 5. Area/Scale Check (Has the card grown or shrunk by > 10%?)
        val hasScaleChanged = lastFocusArea.let { lastArea ->
            if (lastArea == 0f) true
            else {
                val areaChange = abs(currentArea - lastArea) / lastArea
                areaChange > 0.10f // 10% threshold
            }
        }

        // Trigger if cooldown is over AND (Position changed OR Scale changed)
        if (isCooldownOver && (hasMovedSignificantly || hasScaleChanged)) {
            val factory = previewView.meteringPointFactory
            val point = factory.createPoint(centerX, centerY)

            // ADDED FLAG_AE for exposure correction
            val action = FocusMeteringAction.Builder(point, FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE)
                .setAutoCancelDuration(3, java.util.concurrent.TimeUnit.SECONDS)
                .build()

            try {
                control.startFocusAndMetering(action)

                // ADDED: Haptic Feedback (Optional but feels great)
                // previewView.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)

                lastFocusCenter = PointF(centerX, centerY)
                lastFocusArea = currentArea
                lastFocusTimestamp = currentTime
            } catch (e: Exception) {
                Log.e("CAM", "Focus failed", e)
            }
        }
    }
}