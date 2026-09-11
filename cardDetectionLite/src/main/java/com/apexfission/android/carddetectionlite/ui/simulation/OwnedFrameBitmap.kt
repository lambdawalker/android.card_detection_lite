package com.apexfission.android.carddetectionlite.ui.simulation

import android.graphics.Bitmap
import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicBoolean

/** Owns a simulator frame until it is safely recycled or transferred. */
internal class OwnedFrameBitmap(
    val bitmap: Bitmap,
) {
    private val recycled = AtomicBoolean(false)

    fun recycle() {
        if (recycled.compareAndSet(false, true)) {
            bitmap.recycle()
        }
    }
}

/**
 * Transfers [bitmap] ownership to [onBitmap] when callback execution begins.
 * If the executor rejects the callback, ownership stays here and the bitmap is recycled.
 */
internal fun Executor.executeTransferring(
    bitmap: Bitmap,
    onBitmap: (Bitmap) -> Unit,
) {
    val callbackStarted = AtomicBoolean(false)
    try {
        execute {
            callbackStarted.set(true)
            onBitmap(bitmap)
        }
    } catch (t: Throwable) {
        if (!callbackStarted.get()) {
            bitmap.recycle()
        }
        throw t
    }
}
