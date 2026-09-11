package com.apexfission.android.carddetectionlite.ui.detector

import androidx.annotation.WorkerThread
import com.apexfission.android.carddetectionlite.domain.tflite.model.CardDetection
import com.apexfission.android.carddetectionlite.resource.BitmapTransfer

/** Receives continuous card detections on a library-managed worker thread. */
fun interface CardDetectionCallback {
    /**
     * Handles a detection without blocking the library pipeline.
     *
     * Call [BitmapTransfer.takeCopy] before this method returns to take ownership of the bitmap.
     */
    @WorkerThread
    fun onCardDetection(card: CardDetection, bitmapTransfer: BitmapTransfer)
}

/** Receives explicit captures on a library-managed worker thread. */
fun interface CardCaptureCallback {
    /**
     * Handles a capture without blocking the library pipeline.
     *
     * Call [BitmapTransfer.takeCopy] before this method returns to take ownership of the bitmap.
     */
    @WorkerThread
    fun onCapture(card: CardDetection, bitmapTransfer: BitmapTransfer)
}
