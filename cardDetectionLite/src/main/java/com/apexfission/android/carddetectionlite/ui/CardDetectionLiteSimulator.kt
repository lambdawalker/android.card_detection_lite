package com.apexfission.android.carddetectionlite.ui

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.net.Uri
import android.util.Log
import android.view.Surface
import android.view.TextureView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.exoplayer.ExoPlayer
import com.apexfission.android.carddetectionlite.domain.coordinates.ImageSpace
import com.apexfission.android.carddetectionlite.domain.coordinates.ImageSpaceChain
import com.apexfission.android.carddetectionlite.domain.tflite.detector.InputShape
import com.apexfission.android.carddetectionlite.domain.tflite.filters.AspectRatioValidator
import com.apexfission.android.carddetectionlite.domain.tflite.filters.CardValidator
import com.apexfission.android.carddetectionlite.domain.tflite.filters.MarginValidator
import com.apexfission.android.carddetectionlite.domain.tflite.model.CardDetection
import kotlin.math.max
import kotlinx.coroutines.flow.MutableStateFlow

@Composable
fun CardDetectionLiteSimulator(
    modifier: Modifier = Modifier,
    videoUri: Uri,
    modelPath: String,
    classLabels: Map<Int, String>,
    cardClasses: Set<Int>,
    isDetectionEnabled: Boolean,
    useGpu: Boolean = true,
    showBoundingBoxes: Boolean = false,
    showClassNames: Boolean = false,
    showLockOnProgress: Boolean = true,
    scoreThreshold: Float = 0.65f,
    cardFilters: List<CardValidator> = listOf(
        MarginValidator(), AspectRatioValidator()
    ),
    onCardDetection: (CardDetection) -> Unit,
    imageMode: InputShape = InputShape.SquareCrop,
    inferenceIntervalMs: Long = 33L,
    lockOnThreshold: Int = 4,
    numThreads: NumThreads = NumThreads.Default
) {
    val context = LocalContext.current
    val sizeInPixels = remember { MutableStateFlow(IntSize.Zero) }

    val viewModel: CardDetectionLiteSimulatorViewModel = viewModel(
        factory = CardDetectionLiteSimulatorViewModelFactory(
            application = context.applicationContext as Application,
            modelPath = modelPath,
            cardClasses = cardClasses,
            useGpu = useGpu,
            scoreThreshold = scoreThreshold,
            cardFilters = cardFilters,
            canvasSize = sizeInPixels,
            imageMode = imageMode,
            inferenceIntervalMs = inferenceIntervalMs,
            lockOnThreshold = lockOnThreshold,
            numThreads = numThreads,
        )
    )

    LaunchedEffect(isDetectionEnabled) {
        viewModel.setDetectionEnabled(isDetectionEnabled)
    }

    val cardDetection by viewModel.cardDetection.collectAsStateWithLifecycle()
    val scalingInfo by viewModel.scalingInfo.collectAsStateWithLifecycle()

    Box(
        modifier
            .fillMaxSize()
            .onSizeChanged { size ->
                sizeInPixels.value = size
            }) {

        VideoPreviewWithFrameCapture(
            videoUri = videoUri,
            modifier = Modifier.fillMaxSize(),
            onFrame = { bitmap ->
                viewModel.processBitmap(bitmap, onCardDetection)
            }
        )

        if (isDetectionEnabled && showBoundingBoxes && scalingInfo.fullW > 0) {
            DetectionOverlay2(
                cardDetection = cardDetection,
                scalingInfo = scalingInfo,
                showClassNames = showClassNames,
                classLabels = classLabels
            )
        }

        if (isDetectionEnabled && showLockOnProgress && scalingInfo.fullW > 0) {
            CardLockOnOverlay(
                activeDetection = cardDetection,
                scalingInfo = scalingInfo
            )
        }
    }
}

@Composable
fun VideoPreviewWithFrameCapture(
    videoUri: Uri,
    modifier: Modifier = Modifier,
    onFrame: (Bitmap) -> Unit
) {
    val context = LocalContext.current
    val textureView = remember { TextureView(context) }
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(videoUri))
            prepare()
            playWhenReady = true
            repeatMode = Player.REPEAT_MODE_ONE
            addListener(object : Player.Listener {
                override fun onVideoSizeChanged(videoSize: VideoSize) {
                    val viewWidth = textureView.width.toFloat()
                    val viewHeight = textureView.height.toFloat()
                    val videoWidth = videoSize.width.toFloat()
                    val videoHeight = videoSize.height.toFloat()

                    val mainImageSpace = ImageSpace(
                        width = videoWidth.toUInt(),
                        height = videoHeight.toUInt()
                    )

                    if (viewWidth == 0f || viewHeight == 0f || videoWidth == 0f || videoHeight == 0f) {
                        return
                    }

                    val matrix = Matrix()
                    val scaleX = viewWidth / videoWidth
                    val scaleY = viewHeight / videoHeight
                    val scale = maxOf(scaleX, scaleY, 1F)

                    val scaledWidth = videoWidth * scale
                    val scaledHeight = videoHeight * scale

                    val scaledMainImageSpace = ImageSpace(
                        width = scaledWidth.toUInt(),
                        height = scaledHeight.toUInt(),
                        xScale = scale,
                        yScale = scale
                    )

                    val previewImageSpace = ImageSpace(
                        width = videoWidth.toUInt(),
                        height = videoHeight.toUInt(),
                        xOffset = ((viewWidth - scaledWidth) / 2).toUInt(),
                        yOffset = ((viewHeight - scaledHeight) / 2).toUInt()
                    )


                    val imageSpaceChain = listOf(mainImageSpace, scaledMainImageSpace, previewImageSpace)


                    val dx = (viewWidth - scaledWidth) / 2
                    val dy = (viewHeight - scaledHeight) / 2

                    matrix.setScale(scaledWidth / viewWidth, scaledHeight / viewHeight)
                    matrix.postTranslate(dx, dy)

                    textureView.setTransform(matrix)
                }
            })
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    AndroidView(
        factory = { textureView },
        modifier = modifier
    ) { view ->
        view.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
                exoPlayer.setVideoSurface(Surface(surface))
            }

            override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {}

            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean = true

            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
                val bitmap = view.bitmap
                if (bitmap != null) {
                    val copiedBitmap = bitmap.copy(bitmap.config ?: Bitmap.Config.ARGB_8888, true)
                    onFrame(copiedBitmap)
                }
            }
        }
    }
}
