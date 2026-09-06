//package com.apexfission.android.carddetectionlite.ui
//
//import android.graphics.Bitmap
//import android.graphics.Canvas
//import android.graphics.ImageFormat
//import android.graphics.Paint
//import android.graphics.SurfaceTexture
//import android.media.Image
//import android.media.ImageReader
//import android.net.Uri
//import android.view.Surface
//import android.view.TextureView
//import android.view.View
//import androidx.compose.runtime.Composable
//import androidx.compose.runtime.DisposableEffect
//import androidx.compose.runtime.mutableStateOf
//import androidx.compose.runtime.remember
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.platform.LocalContext
//import androidx.compose.ui.viewinterop.AndroidView
//import androidx.lifecycle.Lifecycle
//import androidx.lifecycle.LifecycleEventObserver
//import androidx.lifecycle.compose.LocalLifecycleOwner
//import androidx.media3.common.MediaItem
//import androidx.media3.common.Player
//import androidx.media3.common.VideoSize
//import androidx.media3.exoplayer.ExoPlayer
//import com.apexfission.android.carddetectionlite.domain.coordinates.models.ImageSpace
//import com.apexfission.android.carddetectionlite.domain.coordinates.models.ImageSpaceChain
//import com.apexfission.android.carddetectionlite.domain.coordinates.models.scale
//import kotlinx.coroutines.flow.MutableStateFlow
//
//
//@Composable
//fun VideoPreviewWithFrameCapture3(
//    videoUri: Uri,
//    modifier: Modifier = Modifier,
//    onFrame: (Bitmap, List<ImageSpace>) -> Unit
//) {
//    val context = LocalContext.current
//    val imageReader = remember { mutableStateOf<ImageReader?>(null) }
//    val videoSize = remember { mutableStateOf<VideoSize?>(null) }
//    val latestBitmap = remember { mutableStateOf<Bitmap?>(null) }
//    val imageSpaceChainFlow = remember { MutableStateFlow<ImageSpaceChain>(emptyList()) }
//
//    val exoPlayer = remember {
//        ExoPlayer.Builder(context).build().apply {
//            setMediaItem(MediaItem.fromUri(videoUri))
//            prepare()
//            playWhenReady = true
//            repeatMode = Player.REPEAT_MODE_ONE
//
//            addListener(object : Player.Listener {
//                override fun onVideoSizeChanged(videoSize: VideoSize) {
//                    // Store video size and create ImageReader with these dimensions
//                    this@apply.videoSize.value = videoSize
//                    val width = videoSize.width
//                    val height = videoSize.height
//                    if (width > 0 && height > 0) {
//                        val reader = ImageReader.newInstance(width, height, ImageFormat.YUV_420_888, 2)
//                        reader.setOnImageAvailableListener({ reader ->
//                            val image = reader.acquireLatestImage() ?: return@setOnImageAvailableListener
//                            // Convert Image to Bitmap (original resolution)
//                            val bitmap = imageToBitmap(image)
//                            image.close()
//                            if (bitmap != null) {
//                                latestBitmap.value = bitmap
//                                // Build ImageSpace chain (original → scaled → cropped) for detection overlay
//                                val viewWidth = /* get view size */  // you need to pass this in
//                                val viewHeight = /* get view size */
//                                val imgSpc0 = ImageSpace(width = videoSize.width.toUInt(), height = videoSize.height.toUInt())
//                                val scale = maxOf(viewWidth / width.toFloat(), viewHeight / height.toFloat(), 1F)
//                                val imgSpc1 = imgSpc0.scale(scale)
//                                val imgSpc2 = imgSpc1.cropAtCenter(viewWidth.toUInt(), viewHeight.toUInt())
//                                imageSpaceChainFlow.value = listOf(imgSpc0, imgSpc1, imgSpc2)
//
//                                onFrame(bitmap, imageSpaceChainFlow.value)
//                            }
//                        }, null)
//                        exoPlayer.setVideoSurface(reader.surface)
//                        imageReader.value = reader
//                    }
//                }
//            })
//        }
//    }
//
//    DisposableEffect(Unit) {
//        onDispose {
//            exoPlayer.release()
//            imageReader.value?.close()
//        }
//    }
//
//    // Custom view that draws the latest bitmap with scaling/cropping
//    AndroidView(
//        factory = { ctx ->
//            object : View(ctx) {
//                private val paint = Paint(Paint.FILTER_BITMAP_FLAG)
//                override fun onDraw(canvas: Canvas) {
//                    super.onDraw(canvas)
//                    val bitmap = latestBitmap.value ?: return
//                    val viewWidth = width.toFloat()
//                    val viewHeight = height.toFloat()
//                    val bmpWidth = bitmap.width.toFloat()
//                    val bmpHeight = bitmap.height.toFloat()
//                    if (viewWidth == 0f || viewHeight == 0f) return
//
//                    // Compute same scale and crop as before
//                    val scale = maxOf(viewWidth / bmpWidth, viewHeight / bmpHeight, 1F)
//                    val scaledWidth = bmpWidth * scale
//                    val scaledHeight = bmpHeight * scale
//                    val dx = (viewWidth - scaledWidth) / 2
//                    val dy = (viewHeight - scaledHeight) / 2
//
//                    canvas.save()
//                    canvas.translate(dx, dy)
//                    canvas.scale(scaledWidth / bmpWidth, scaledHeight / bmpHeight)
//                    canvas.drawBitmap(bitmap, 0f, 0f, paint)
//                    canvas.restore()
//                }
//            }
//        },
//        modifier = modifier
//    )
//}
//
//// Helper to convert YUV_420_888 Image to Bitmap
//fun imageToBitmap(image: Image): Bitmap? {
//    // Implement conversion using ImageUtil or custom YUV to RGB
//    // (Standard boilerplate – search for "YUV_420_888 to Bitmap")
//}