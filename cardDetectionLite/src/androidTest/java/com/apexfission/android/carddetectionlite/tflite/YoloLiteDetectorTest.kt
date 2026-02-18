package com.apexfission.android.carddetectionlite.tflite

import android.graphics.BitmapFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.apexfission.android.carddetectionlite.Det
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.InputStream
import kotlin.math.max
import kotlin.math.min

@RunWith(AndroidJUnit4::class)
class YoloLiteDetectorTest {

    private lateinit var detector: YoloLiteDetector
    private val iouThreshold = 0.85f
    private val maxInferenceTime = 100L

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        detector = YoloLiteDetector(
            context = context,
            modelName = "E38Y11_640B1F16.tflite",
            scoreThreshold = 0.5f,
            iouThreshold = 0.5f
        )
    }

    @After
    fun tearDown() {
        detector.close()
    }

    @Test
    fun detect_4_jpg() {
        runDetectionTest("test/images/4.jpg", "test/images/4.txt")
    }

    @Test
    fun detect_5_jpg() {
        runDetectionTest("test/images/5.jpg", "test/images/5.txt")
    }

    @Test
    fun detect_6_jpg() {
        runDetectionTest("test/images/6.jpg", "test/images/6.txt")
    }

    @Test
    fun detect_12_jpg() {
        runDetectionTest("test/images/12.jpg", "test/images/12.txt")
    }

    private fun runDetectionTest(imagePath: String, gtPath: String) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val imageStream: InputStream = context.assets.open(imagePath)
        val bitmap = BitmapFactory.decodeStream(imageStream)

        val gtStream: InputStream = context.assets.open(gtPath)
        val gtContent = gtStream.bufferedReader().use { it.readText() }
        val gtParts = gtContent.split(" ")
        val gtDet = Det(
            x1 = (gtParts[1].toFloat() - gtParts[3].toFloat() / 2),
            y1 = (gtParts[2].toFloat() - gtParts[4].toFloat() / 2),
            x2 = (gtParts[1].toFloat() + gtParts[3].toFloat() / 2),
            y2 = (gtParts[2].toFloat() + gtParts[4].toFloat() / 2),
            score = 1.0f,
            cls = gtParts[0].toInt()
        )

        detector.enabled = true
        val detections = detector.detect(bitmap)

        assertTrue("No detections found in $imagePath", detections.isNotEmpty())
        assertTrue("Inference time too high: ${detector.lastInferenceTimeMs}ms", detector.lastInferenceTimeMs <= maxInferenceTime)

        val bestMatch = detections.maxByOrNull { iou(it, gtDet) }
        assertTrue("No detection passed IOU threshold of $iouThreshold", bestMatch != null && iou(bestMatch, gtDet) >= iouThreshold)
    }

    private fun iou(a: Det, b: Det): Float {
        val interW = max(0f, min(a.x2, b.x2) - max(a.x1, b.x1))
        val interH = max(0f, min(a.y2, b.y2) - max(a.y1, b.y1))
        val inter = interW * interH
        val areaA = (a.x2 - a.x1) * (a.y2 - a.y1)
        val areaB = (b.x2 - b.x1) * (b.y2 - b.y1)
        return inter / (areaA + areaB - inter + 1e-6f)
    }
}