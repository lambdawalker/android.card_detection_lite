package com.apexfission.android.carddetectionlite.ui.simulation

import android.graphics.Bitmap
import android.net.Uri
import android.view.TextureView
import androidx.annotation.OptIn
import androidx.compose.foundation.border
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.ByteBufferGlEffect
import androidx.media3.effect.Presentation
import androidx.media3.exoplayer.ExoPlayer
import com.apexfission.android.carddetectionlite.domain.coordinates.models.ImageSpaceChain
import com.apexfission.android.carddetectionlite.ui.overlays.createPreviewImageSpaceChain
import java.util.concurrent.atomic.AtomicReference

/**
 * A video preview composable that renders video output via ExoPlayer and captures full frames
 * along with their corresponding [ImageSpaceChain] coordinate mapping.
 *
 * @param videoUri Source video URI.
 * @param modifier Composable modifier.
 * @param captureIntervalMs Minimum interval between captured bitmap frames in milliseconds.
 * @param onFrame Callback invoked with each captured frame [Bitmap] and its [ImageSpaceChain].
 */
@OptIn(UnstableApi::class)
@Composable
fun VideoPreviewWithFullFrameCapture(
    videoUri: Uri, modifier: Modifier = Modifier, captureIntervalMs: Long = 33L, onFrame: (Bitmap, ImageSpaceChain) -> Unit
) {
    val context = LocalContext.current
    val mainExecutor = remember(context) { ContextCompat.getMainExecutor(context) }
    val textureView = remember { TextureView(context) }

    val onFrameRef = remember { AtomicReference(onFrame) }
    val imageSpaceChainRef = remember {
        AtomicReference<ImageSpaceChain>(ImageSpaceChain.Empty)
    }

    var sourceSize by remember(videoUri) { mutableStateOf(IntSize.Zero) }
    var viewSize by remember { mutableStateOf(IntSize.Zero) }

    SideEffect {
        onFrameRef.set(onFrame)
    }

    val frameProcessor = remember(videoUri, captureIntervalMs) {
        BitmapFrameProcessor(captureIntervalMs = captureIntervalMs, callbackExecutor = mainExecutor, onConfigured = { width, height ->
            mainExecutor.execute {
                sourceSize = IntSize(width, height)
            }
        }, onBitmap = { bitmap, presentationTimeUs ->
            onFrameRef.get().invoke(
                bitmap, imageSpaceChainRef.get()
            )
        })
    }

    val frameCaptureEffect = remember(frameProcessor) {
        ByteBufferGlEffect<Unit>(frameProcessor)
    }

    val exoPlayer = remember(videoUri, frameCaptureEffect, textureView) {
        ExoPlayer.Builder(context).build().apply {
            setVideoEffects(listOf(frameCaptureEffect))
            setVideoTextureView(textureView)
            setMediaItem(MediaItem.fromUri(videoUri))
            repeatMode = Player.REPEAT_MODE_ONE
            prepare()
            playWhenReady = true
        }
    }

    LaunchedEffect(exoPlayer, frameCaptureEffect, viewSize) {
        if (viewSize == IntSize.Zero) return@LaunchedEffect

        val aspectRatio = viewSize.width.toFloat() / viewSize.height.toFloat()

        val previewCrop = Presentation.createForAspectRatio(
            aspectRatio, Presentation.LAYOUT_SCALE_TO_FIT_WITH_CROP
        )

        exoPlayer.setVideoEffects(
            listOf(
                frameCaptureEffect, previewCrop
            )
        )
    }

    LaunchedEffect(sourceSize, viewSize) {
        if (sourceSize == IntSize.Zero || viewSize == IntSize.Zero) {
            imageSpaceChainRef.set(ImageSpaceChain.Empty)
            return@LaunchedEffect
        }

        val imageSpaceChain = createPreviewImageSpaceChain(
            videoWidth = sourceSize.width, videoHeight = sourceSize.height, viewWidth = viewSize.width, viewHeight = viewSize.height
        )

        imageSpaceChainRef.set(imageSpaceChain)
    }

    DisposableEffect(exoPlayer) {
        onDispose {
            exoPlayer.clearVideoTextureView(textureView)
            exoPlayer.release()
        }
    }

    AndroidView(
        factory = { textureView }, modifier = modifier
            .border(0.5.dp, Color.Green)
            .onSizeChanged { viewSize = it })
}
