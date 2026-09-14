package com.apexfission.android.carddetectionlite.domain.tflite.detector.tflite.engine

import android.graphics.Bitmap
import androidx.camera.core.ImageProxy
import com.apexfission.android.carddetectionlite.domain.coordinates.models.ImageBox
import com.apexfission.android.carddetectionlite.domain.tflite.detector.card.engine.CardDetector
import com.apexfission.android.carddetectionlite.domain.tflite.detector.card.engine.ThreadConfinedCardDetector
import com.apexfission.android.carddetectionlite.domain.tflite.detector.yolo.engine.Detector
import com.apexfission.android.carddetectionlite.domain.tflite.detector.yolo.engine.ThreadConfinedYoloDetector
import com.apexfission.android.carddetectionlite.domain.tflite.model.CardDetection
import com.apexfission.android.carddetectionlite.domain.tflite.model.Detection
import com.apexfission.android.carddetectionlite.domain.tflite.model.Feature
import com.apexfission.android.carddetectionlite.domain.tflite.model.LockingStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.mock

class SharedEngineDispatcherTest {

    @Test
    fun `shared EngineThreadDispatcher executes all 3 pipeline wrappers on the exact same thread`() {
        val sharedEngineDispatcher = EngineThreadDispatcher()

        val mockInferenceEngine = object : InferenceEngine {
            override val isInt8: Boolean = false
            override val inputImageWidth: Int = 640
            override val outLayout: InferenceEngine.OutputLayout = InferenceEngine.OutputLayout.ATTRS_X_BOXES
            override val outAttrs: Int = 85
            override val outBoxes: Int = 8400
            override val numClasses: Int = 81
            override val lastInferenceTimeMs: Long = 10L

            override fun runInference(bitmap: Bitmap): FloatArray = floatArrayOf()
            override fun close() {}
        }

        val mockDetector = object : Detector {
            override var enabled: Boolean = true
            override fun detect(bitmap: Bitmap): List<Detection> = emptyList()
            override fun detect(imageProxy: ImageProxy): List<Detection> = emptyList()
            override fun close() {}
        }

        val mockCardDetector = object : CardDetector {
            override var enabled: Boolean = true
            override fun track(imageProxy: ImageProxy): CardDetection? = null
            override fun track(bitmap: Bitmap): CardDetection = CardDetection(
                id = 1L,
                lockingStatus = LockingStatus.CardLocked,
                card = Feature(ImageBox.from2P(0, 0, 100, 100), 0.9f, 0),
                features = emptyList(),
                lockOnProgress = 1.0f
            )
            override fun close() {}
        }

        val inferenceEngineWrapper = ThreadConfinedInferenceEngine(sharedEngineDispatcher) {
            mockInferenceEngine
        }

        val yoloDetectorWrapper = ThreadConfinedYoloDetector(sharedEngineDispatcher) {
            mockDetector
        }

        val cardDetectorWrapper = ThreadConfinedCardDetector(sharedEngineDispatcher) {
            mockCardDetector
        }

        // Verify all 3 wrappers initialized on the exact same worker thread
        val engineThreadId = inferenceEngineWrapper.engineThreadId
        val yoloThreadId = yoloDetectorWrapper.detectorThreadId
        val cardThreadId = cardDetectorWrapper.detectorThreadId

        assertEquals("InferenceEngine and YoloDetector must share thread ID", engineThreadId, yoloThreadId)
        assertEquals("YoloDetector and CardDetector must share thread ID", yoloThreadId, cardThreadId)

        val bitmap = mock<Bitmap>()
        inferenceEngineWrapper.runInference(bitmap)
        yoloDetectorWrapper.detect(bitmap)
        cardDetectorWrapper.track(bitmap)

        assertEquals("Inference runInference thread", engineThreadId, inferenceEngineWrapper.lastInferenceThreadId)
        assertEquals("Yolo detect thread", engineThreadId, yoloDetectorWrapper.lastDetectThreadId)
        assertEquals("Card track thread", engineThreadId, cardDetectorWrapper.lastTrackThreadId)

        cardDetectorWrapper.close()
        yoloDetectorWrapper.close()
        inferenceEngineWrapper.close()

        sharedEngineDispatcher.close()
        assertTrue("Shared executor should be shut down after close()", sharedEngineDispatcher.isShutdown)
    }
}
