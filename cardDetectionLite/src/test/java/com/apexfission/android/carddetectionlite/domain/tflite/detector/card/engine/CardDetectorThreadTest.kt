package com.apexfission.android.carddetectionlite.domain.tflite.detector.card.engine

import android.graphics.Bitmap
import androidx.camera.core.ImageProxy
import com.apexfission.android.math.models.ImageBox
import com.apexfission.android.carddetectionlite.domain.tflite.model.CardDetection
import com.apexfission.android.carddetectionlite.domain.tflite.model.Feature
import com.apexfission.android.carddetectionlite.domain.tflite.model.LockingStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.mock

class CardDetectorThreadTest {

    @Test
    fun `ThreadConfinedCardDetector confines track and close calls to dedicated thread`() {
        var factoryCalled = false
        var trackCalled = false
        var closeCalled = false

        val mockDetector = object : CardDetector {
            override var enabled: Boolean = true

            override fun track(imageProxy: ImageProxy): CardDetection? = null

            override fun track(bitmap: Bitmap): CardDetection {
                trackCalled = true
                return CardDetection(
                    id = 1L,
                    lockingStatus = LockingStatus.CardLocked,
                    card = Feature(ImageBox.from2P(0, 0, 100, 100), 0.9f, 0),
                    features = emptyList(),
                    lockOnProgress = 1.0f
                )
            }

            override fun close() {
                closeCalled = true
            }
        }

        val wrapper = ThreadConfinedCardDetector {
            factoryCalled = true
            mockDetector
        }

        assertTrue("Factory must be invoked on init", factoryCalled)
        assertTrue("Enabled state must be delegated", wrapper.enabled)

        val bitmap = mock<Bitmap>()
        val result = wrapper.track(bitmap)

        assertTrue("Track must be delegated", trackCalled)
        assertEquals(1L, result?.id)

        wrapper.close()
        assertTrue("Close must be delegated", closeCalled)
    }
}
