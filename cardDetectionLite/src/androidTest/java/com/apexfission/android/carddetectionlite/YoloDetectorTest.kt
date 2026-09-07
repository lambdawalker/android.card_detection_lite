package com.apexfission.android.carddetectionlite

import android.graphics.BitmapFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.apexfission.android.carddetectionlite.domain.ModelCatalog
import com.apexfission.android.carddetectionlite.domain.tflite.detector.YoloDetector
import com.apexfission.android.carddetectionlite.domain.tflite.model.Detection
import com.apexfission.android.carddetectionlite.tfmodel.modelPath
import com.apexfission.android.carddetectionlite.ui.detector.NumThreads
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith


/**
 * Instrumented test for [YoloDetector] using test images and ground truth annotations
 * from assets (`test/images/`).
 */
@RunWith(AndroidJUnit4::class)
class YoloDetectorTest {
    @Test
    @GenerateImage
    fun testDetectSingleImage() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        val detector = YoloDetector(
            context = context,
            modelPath = ModelCatalog.TfLite.modelPath,
            scoreThreshold = 0.3f,
            iouThreshold = 0.45f,
            useGpu = false,
            numThreads = NumThreads.Default
        )

        val imageName = "4"
        val bitmap = context.assets.open("test/images/$imageName.jpg").use { inputStream ->
            BitmapFactory.decodeStream(inputStream)
        }

        assertNotNull("Failed to load test image $imageName.jpg", bitmap)

        val rawGroundTruth = context.assets.open("test/images/$imageName.txt").bufferedReader().use {
            it.readText()
        }
        val groundTruth = parseGroundTruth(rawGroundTruth)
        val expectedBox = groundTruth.toPixelBox(bitmap.width, bitmap.height)


        detector.use { detector ->
            val detections: List<Detection> = detector.detect(bitmap)

            assertTrue("Expected at least one detection for $imageName.jpg", detections.isNotEmpty())

            val matchingDetection = detections.firstOrNull { it.classId == groundTruth.classId }
            assertNotNull(
                "Expected detection with classId ${groundTruth.classId} for $imageName.jpg, but got: ${detections.map { it.classId }}",
                matchingDetection
            )

            val iou = calculateIoU(matchingDetection!!.box, expectedBox)
            assertTrue(
                "IoU between detected box (${matchingDetection.box}) and ground truth ($expectedBox) should be >= 0.5, but was $iou", iou >= 0.5f
            )

            val resultBitmap = drawBoxesOnBitmap(bitmap, expectedBox, matchingDetection.box)
            autoSaveBitmap(resultBitmap, imageName)
        }
    }
}
