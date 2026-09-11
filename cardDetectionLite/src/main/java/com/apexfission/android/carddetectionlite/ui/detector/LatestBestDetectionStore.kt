package com.apexfission.android.carddetectionlite.ui.detector

import android.graphics.Bitmap
import com.apexfission.android.carddetectionlite.domain.tflite.model.CardDetection
import com.apexfission.android.carddetectionlite.domain.tflite.model.LockingStatus
import com.apexfission.android.carddetectionlite.resource.BitmapTransfer
import com.apexfission.android.carddetectionlite.resource.withBitmapTransfer
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Thread-safe owner of the highest-confidence image retained for capture. */
internal class LatestBestDetectionStore(
    private val bitmapCopyDispatcher: CoroutineDispatcher = Dispatchers.Default,
) {
    private data class Entry(
        val detection: CardDetection,
        val bitmap: Bitmap,
    )

    private val lock = Any()
    private var entry: Entry? = null

    fun offer(detection: CardDetection, source: Bitmap): Boolean {
        var replaced: Entry? = null
        val accepted = synchronized(lock) {
            val current = entry
            if (!shouldReplace(current?.detection, detection)) {
                false
            } else {
                val ownedCopy = checkNotNull(
                    source.copy(source.config ?: Bitmap.Config.ARGB_8888, false)
                ) { "Unable to copy the detected card bitmap." }
                replaced = current
                entry = Entry(detection, ownedCopy)
                true
            }
        }

        replaced?.bitmap?.recycle()
        return accepted
    }

    fun detection(): CardDetection? = synchronized(lock) { entry?.detection }

    suspend fun withTransfer(block: (CardDetection, BitmapTransfer) -> Unit): Boolean {
        var snapshot: Entry? = null
        try {
            withContext(bitmapCopyDispatcher) {
                snapshot = synchronized(lock) {
                    entry?.let { current ->
                        val copy = checkNotNull(
                            current.bitmap.copy(
                                current.bitmap.config ?: Bitmap.Config.ARGB_8888,
                                false,
                            )
                        ) { "Unable to copy the retained card bitmap." }
                        Entry(current.detection, copy)
                    }
                }
            }
        } catch (throwable: Throwable) {
            snapshot?.bitmap?.recycle()
            throw throwable
        }

        val captured = snapshot ?: return false

        withBitmapTransfer(captured.bitmap) { transfer ->
            block(captured.detection, transfer)
        }
        return true
    }

    fun clear() {
        val previous = synchronized(lock) {
            val current = entry
            entry = null
            current
        }
        previous?.bitmap?.recycle()
    }

    private fun shouldReplace(
        current: CardDetection?,
        candidate: CardDetection,
    ): Boolean {
        if (current == null) return true
        if (candidate.lockingStatus == LockingStatus.NewCard && candidate.id != current.id) {
            return true
        }
        return candidate.card.confidence > current.card.confidence
    }
}
