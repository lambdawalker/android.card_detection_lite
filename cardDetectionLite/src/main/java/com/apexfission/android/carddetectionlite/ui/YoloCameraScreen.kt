package com.apexfission.android.carddetectionlite.ui

import android.app.Application
import android.graphics.Bitmap
import android.util.Log
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.MeteringPoint
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.apexfission.android.carddetectionlite.Det
import java.util.concurrent.Executors
import kotlin.math.max

class YoloCameraViewModelFactory(
    private val application: Application, private val modelName: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(YoloCameraViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST") return YoloCameraViewModel(application, modelName) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

@Composable
fun YoloCameraScreen(
    modelName: String,
    isDetectionEnabled: Boolean,
    showBoundingBoxes: Boolean = true,
    showClassNames: Boolean = true,
    classLabels: Map<Int, String> = emptyMap(),
    onDetections: (List<Bitmap>) -> Unit,
) {
    val context = LocalContext.current
    val viewModel: YoloCameraViewModel = viewModel(
        factory = YoloCameraViewModelFactory(context.applicationContext as Application, modelName)
    )

    LaunchedEffect(isDetectionEnabled) {
        viewModel.setDetectionEnabled(isDetectionEnabled)
    }

    val flashlightEnabled by viewModel.flashlightEnabled.collectAsStateWithLifecycle()
    val detections by viewModel.detections.collectAsStateWithLifecycle()
    val scalingInfo by viewModel.scalingInfo.collectAsStateWithLifecycle()

    Box(Modifier.fillMaxSize()) {
        CameraPreview(
            lifecycleOwner = LocalLifecycleOwner.current,
            onFrame = { imageProxy -> viewModel.processImage(imageProxy, onDetections) },
            onFocusEvent = { cameraControl, meteringPoint -> viewModel.onFocusEvent(cameraControl, meteringPoint) },
            flashlightEnabled = flashlightEnabled,
        )

        if (isDetectionEnabled && showBoundingBoxes && scalingInfo.fullW > 0) {
            DetectionOverlay(
                detections = detections, scalingInfo = scalingInfo, showClassNames = showClassNames, classLabels = classLabels
            )
        }

        Switch(
            checked = flashlightEnabled, onCheckedChange = { viewModel.toggleFlashlight() }, modifier = Modifier.padding(16.dp)
        )
    }
}

@Composable
fun DetectionOverlay(
    detections: List<Det>, scalingInfo: PreviewScalingInfo, showClassNames: Boolean, classLabels: Map<Int, String>
) {
    val textMeasurer = rememberTextMeasurer()
    val textStyle = TextStyle(color = Color.White, fontSize = 12.sp, background = Color.Black.copy(alpha = 0.5f))

    Canvas(modifier = Modifier.fillMaxSize()) {
        val screenW = size.width
        val screenH = size.height

        // Calculate how the full camera buffer maps to the screen given FILL_CENTER
        val scale = max(screenW / scalingInfo.fullW, screenH / scalingInfo.fullH)
        val offsetX = (screenW - scalingInfo.fullW * scale) / 2f
        val offsetY = (screenH - scalingInfo.fullH * scale) / 2f

        // The detector results are [0..1] relative to the CROP rectangle.
        // First we map to crop pixels, then we offset by the crop's position in the full buffer.
        // However, since rotateRectToUpright centers the crop, we need to be careful.
        // Assuming the crop covers the whole area used by FILL_CENTER:
        detections.forEach { det ->
            // Scale detection relative to the crop dimensions
            val cropX1 = det.x1 * scalingInfo.cropW
            val cropY1 = det.y1 * scalingInfo.cropH
            val cropX2 = det.x2 * scalingInfo.cropW
            val cropY2 = det.y2 * scalingInfo.cropH

            // Since the detector worked on the crop, and FILL_CENTER crops the buffer to fit screen,
            // we calculate the position relative to the centered preview.
            val x1 = cropX1 * scale + offsetX
            val y1 = cropY1 * scale + offsetY
            val x2 = cropX2 * scale + offsetX
            val y2 = cropY2 * scale + offsetY

            drawRect(
                color = Color.Cyan, topLeft = Offset(x1, y1), size = Size(x2 - x1, y2 - y1), style = Stroke(width = 2.dp.toPx())
            )

            if (showClassNames) {
                val label = classLabels[det.cls] ?: "ID: ${det.cls}"
                val displayString = "$label (${(det.score * 100).toInt()}%)"
                val textLayout = textMeasurer.measure(displayString, textStyle)
                val textTop = if (y1 - textLayout.size.height < 0) y1 else y1 - textLayout.size.height
                drawText(textLayoutResult = textLayout, topLeft = Offset(x1, textTop))
            }
        }
    }
}

@Composable
private fun CameraPreview(
    onFrame: (ImageProxy) -> Unit,
    onFocusEvent: (CameraControl, MeteringPoint) -> Unit,
    lifecycleOwner: LifecycleOwner,
    flashlightEnabled: Boolean,
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
            val previewUseCase = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
            val analysisUseCase = ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setTargetRotation(previewView.display.rotation).build()

            analysisUseCase.setAnalyzer(analysisExecutor) { imageProxy ->
                try {
                    onFrameState.value(imageProxy)
                } catch (t: Throwable) {
                    imageProxy.close()
                }
            }

            try {
                cameraProvider.unbindAll()
                val camera = cameraProvider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, previewUseCase, analysisUseCase)
                camera.cameraControl.enableTorch(flashlightEnabled)
                previewView.setOnTouchListener { _, event ->
                    onFocusEvent(camera.cameraControl, previewView.meteringPointFactory.createPoint(event.x, event.y))
                    true
                }
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