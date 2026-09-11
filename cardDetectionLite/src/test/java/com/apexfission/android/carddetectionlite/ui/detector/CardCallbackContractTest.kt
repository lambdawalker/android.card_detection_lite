package com.apexfission.android.carddetectionlite.ui.detector

import com.apexfission.android.carddetectionlite.domain.tflite.model.CardDetection
import com.apexfission.android.carddetectionlite.resource.BitmapTransfer
import org.junit.Assert.assertSame
import org.junit.Test
import org.mockito.kotlin.mock

class CardCallbackContractTest {

    @Test
    fun detectionCallbackSupportsExplicitSamImplementation() {
        val detection = mock<CardDetection>()
        val transfer = mock<BitmapTransfer>()
        var receivedDetection: CardDetection? = null
        var receivedTransfer: BitmapTransfer? = null
        val callback = CardDetectionCallback { card, bitmapTransfer ->
            receivedDetection = card
            receivedTransfer = bitmapTransfer
        }

        callback.onCardDetection(detection, transfer)

        assertSame(detection, receivedDetection)
        assertSame(transfer, receivedTransfer)
    }

    @Test
    fun captureCallbackSupportsExplicitSamImplementation() {
        val detection = mock<CardDetection>()
        val transfer = mock<BitmapTransfer>()
        var receivedDetection: CardDetection? = null
        var receivedTransfer: BitmapTransfer? = null
        val callback = CardCaptureCallback { card, bitmapTransfer ->
            receivedDetection = card
            receivedTransfer = bitmapTransfer
        }

        callback.onCapture(detection, transfer)

        assertSame(detection, receivedDetection)
        assertSame(transfer, receivedTransfer)
    }
}
