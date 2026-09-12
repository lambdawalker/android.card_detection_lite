package com.apexfission.android.carddetectionlite.ui.detector

import android.graphics.Bitmap
import androidx.annotation.WorkerThread
import com.apexfission.android.carddetectionlite.domain.tflite.model.CardDetection

/** Receives continuous card detections on a library-managed worker thread. */
fun interface CardDetectionCallback {
    /**
     * Handles a detection without blocking the library pipeline.
     *
     * [bitmap] belongs exclusively to the callback recipient, which must recycle it after use.
     */
    @WorkerThread
    fun onCardDetection(card: CardDetection, bitmap: Bitmap)
}

/** Receives explicit captures on a library-managed worker thread. */
fun interface CardCaptureCallback {
    /**
     * Handles a capture without blocking the library pipeline.
     *
     * [bitmap] belongs exclusively to the callback recipient, which must recycle it after use.
     */
    @WorkerThread
    fun onCapture(card: CardDetection, bitmap: Bitmap)
}
