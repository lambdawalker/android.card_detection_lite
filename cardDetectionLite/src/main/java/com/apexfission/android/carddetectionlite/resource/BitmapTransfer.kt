package com.apexfission.android.carddetectionlite.resource

import android.graphics.Bitmap
import java.util.concurrent.atomic.AtomicReference

/**
 * A callback-scoped, one-shot transfer of an independently owned card bitmap.
 *
 * [takeCopy] must be called synchronously while the callback that supplied this transfer is
 * running. The caller becomes the sole owner of the returned bitmap and must recycle it.
 */
interface BitmapTransfer {
    fun takeCopy(): Bitmap
}

internal object BitmapTransferException {
    const val AlreadyConsumed = "BitmapTransfer has already been consumed."
    const val Expired =
        "BitmapTransfer has expired. takeCopy() must be called synchronously inside the callback."
}

internal class OneShotBitmapTransfer(bitmap: Bitmap) : BitmapTransfer {
    private sealed interface State {
        class Available(val bitmap: Bitmap) : State
        data object Consumed : State
        data object Expired : State
    }

    private val state = AtomicReference<State>(State.Available(bitmap))

    override fun takeCopy(): Bitmap {
        while (true) {
            when (val current = state.get()) {
                is State.Available -> {
                    if (state.compareAndSet(current, State.Consumed)) {
                        return current.bitmap
                    }
                }

                State.Consumed -> throw IllegalStateException(BitmapTransferException.AlreadyConsumed)
                State.Expired -> throw IllegalStateException(BitmapTransferException.Expired)
            }
        }
    }

    fun expire() {
        while (true) {
            when (val current = state.get()) {
                is State.Available -> {
                    if (state.compareAndSet(current, State.Expired)) {
                        current.bitmap.recycle()
                        return
                    }
                }

                State.Consumed,
                State.Expired -> return
            }
        }
    }
}

internal inline fun <T> withBitmapTransfer(
    bitmap: Bitmap,
    block: (BitmapTransfer) -> T,
): T {
    val transfer = OneShotBitmapTransfer(bitmap)
    return try {
        block(transfer)
    } finally {
        transfer.expire()
    }
}

/** Executes [block] and always recycles this bitmap afterward. */
inline fun <T> Bitmap.use(block: (Bitmap) -> T): T {
    return try {
        block(this)
    } finally {
        if (!isRecycled) recycle()
    }
}
