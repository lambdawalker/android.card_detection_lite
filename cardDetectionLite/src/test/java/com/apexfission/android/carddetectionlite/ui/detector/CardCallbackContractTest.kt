package com.apexfission.android.carddetectionlite.ui.detector

import android.graphics.Bitmap
import com.apexfission.android.carddetectionlite.domain.tflite.model.CardDetection
import org.junit.Assert.assertSame
import org.junit.Test
import org.mockito.kotlin.mock

class CardCallbackContractTest {

    @Test
    fun detectionCallbackSupportsExplicitSamImplementation() {
        val detection = mock<CardDetection>()
        val bitmap = mock<Bitmap>()
        var receivedDetection: CardDetection? = null
        var receivedBitmap: Bitmap? = null
        val callback = CardDetectionCallback { card, ownedBitmap ->
            receivedDetection = card
            receivedBitmap = ownedBitmap
        }

        callback.onCardDetection(detection, bitmap)

        assertSame(detection, receivedDetection)
        assertSame(bitmap, receivedBitmap)
    }

    @Test
    fun captureCallbackSupportsExplicitSamImplementation() {
        val detection = mock<CardDetection>()
        val bitmap = mock<Bitmap>()
        var receivedDetection: CardDetection? = null
        var receivedBitmap: Bitmap? = null
        val callback = CardCaptureCallback { card, ownedBitmap ->
            receivedDetection = card
            receivedBitmap = ownedBitmap
        }

        callback.onCapture(detection, bitmap)

        assertSame(detection, receivedDetection)
        assertSame(bitmap, receivedBitmap)
    }
}
