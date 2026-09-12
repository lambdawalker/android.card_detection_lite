package com.apexfission.android.carddetectionlite.ui.simulation

import android.graphics.Bitmap
import androidx.media3.common.GlTextureInfo
import androidx.media3.common.util.GlRect
import androidx.media3.common.util.Size
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.ByteBufferGlEffect
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Media3 [ByteBufferGlEffect.Processor] implementation that captures video frames into [Bitmap] instances
 * at a configured time interval.
 *
 * @param captureIntervalMs Interval between captured frames in milliseconds.
 * @param callbackExecutor Executor used for delivering bitmap callbacks.
 * @param onConfigured Callback invoked when video frame dimensions are configured.
 * @param onBitmap Callback invoked when a bitmap frame is captured. Ownership of the bitmap
 * transfers to the callback, which must eventually recycle it.
 */
@UnstableApi
class BitmapFrameProcessor(
    captureIntervalMs: Long,
    callbackExecutor: Executor,
    private val onConfigured: (width: Int, height: Int) -> Unit,
    private val onBitmap: (bitmap: Bitmap, presentationTimeUs: Long) -> Unit,
) : ByteBufferGlEffect.Processor<Unit> {

    private val captureIntervalUs = captureIntervalMs.coerceIn(0L, Long.MAX_VALUE / 1_000L) * 1_000L
    private val lifecycleLock = Any()
    private val released = AtomicBoolean(false)
    private val bitmapCallbacks = ReleaseAwareCallbackDispatcher(
        executor = callbackExecutor,
        releaseUndelivered = Bitmap::recycle,
    )

    private var width = 0
    private var height = 0
    private var lastCaptureTimeUs = Long.MIN_VALUE

    override fun configure(inputWidth: Int, inputHeight: Int): Size {
        width = inputWidth
        height = inputHeight

        synchronized(lifecycleLock) {
            if (!released.get()) onConfigured(inputWidth, inputHeight)
        }

        return Size(inputWidth, inputHeight)
    }

    override fun getScaledRegion(presentationTimeUs: Long): GlRect =
        GlRect(width, height)

    override fun processImage(
        image: ByteBufferGlEffect.Image,
        presentationTimeUs: Long
    ): ListenableFuture<Unit> {
        if (released.get()) return Futures.immediateFuture(Unit)

        if (shouldCaptureFrame(presentationTimeUs)) {
            lastCaptureTimeUs = presentationTimeUs

            // Must copy while Media3's pixel buffer is still valid.
            val bitmap = image.copyToBitmap()

            bitmapCallbacks.submit(bitmap) { transferredBitmap ->
                onBitmap(transferredBitmap, presentationTimeUs)
            }
        }

        return Futures.immediateFuture(Unit)
    }

    private fun shouldCaptureFrame(presentationTimeUs: Long): Boolean {
        if (lastCaptureTimeUs == Long.MIN_VALUE) return true
        if (presentationTimeUs < lastCaptureTimeUs) return true
        if (captureIntervalUs == 0L) return true

        return presentationTimeUs - lastCaptureTimeUs >= captureIntervalUs
    }

    override fun finishProcessingAndBlend(
        outputFrame: GlTextureInfo,
        presentationTimeUs: Long,
        result: Unit
    ) = Unit

    override fun release() {
        val firstRelease = synchronized(lifecycleLock) {
            released.compareAndSet(false, true)
        }
        if (firstRelease) bitmapCallbacks.close()
    }
}
