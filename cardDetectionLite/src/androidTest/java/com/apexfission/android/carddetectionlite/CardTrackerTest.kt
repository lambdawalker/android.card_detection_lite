package com.apexfission.android.carddetectionlite

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.apexfission.android.carddetectionlite.domain.ModelCatalog
import com.apexfission.android.carddetectionlite.domain.tflite.detector.CardTracker
import com.apexfission.android.carddetectionlite.domain.tflite.detector.YoloDetector2
import com.apexfission.android.carddetectionlite.domain.tflite.image.generateDHash
import com.apexfission.android.carddetectionlite.tfmodel.cardClasses
import com.apexfission.android.carddetectionlite.tfmodel.modelPath
import com.apexfission.android.carddetectionlite.ui.NumThreads
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented test for [CardTracker] using frames extracted from `test/video/0.mp4`
 * and ground truth annotations from `test/video/0.txt`.
 */
@RunWith(AndroidJUnit4::class)
class CardTrackerTest {

    private fun extractFramesFromVideo(
        context: Context,
        assetPath: String,
        frameIntervalMs: Long = 100L,
        maxFrames: Int = 30
    ): List<Bitmap> {
        val retriever = MediaMetadataRetriever()
        val afd = context.assets.openFd(assetPath)
        retriever.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
        afd.close()

        val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong() ?: 0L
        val frames = mutableListOf<Bitmap>()

        var currentTimeMs = 0L
        while (currentTimeMs <= durationMs && frames.size < maxFrames) {
            val bitmap = retriever.getFrameAtTime(
                currentTimeMs * 1000L,
                MediaMetadataRetriever.OPTION_CLOSEST_SYNC
            )
            if (bitmap != null) {
                frames.add(bitmap)
            }
            currentTimeMs += frameIntervalMs
        }

        retriever.release()
        return frames
    }

    @Test
    fun testCardDetectionFromVideoFrames() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        val rawGroundTruth = context.assets.open("test/video/A/frame_0_expected_data.txt").bufferedReader().use {
            it.readText()
        }
        val groundTruth = parseGroundTruth(rawGroundTruth)

        val videoPath = "test/video/A/source.mp4"
        Log.d("YoloCardDetector", "Extracting frames from $videoPath")
        val frames = extractFramesFromVideo(context, videoPath, frameIntervalMs = 100L, maxFrames = 15)
        assertTrue("Expected to extract video frames from $videoPath", frames.isNotEmpty())

        Log.d("YoloCardDetector", "Loading Model")
        val yoloDetector = YoloDetector2(
            context = context,
            modelPath = ModelCatalog.TfLite.modelPath,
            scoreThreshold = 0.3f,
            iouThreshold = 0.45f,
            useGpu = false,
            numThreads = NumThreads.Default
        )

        val cardDetector = CardTracker(
            yoloDetector = yoloDetector,
            cardValidators = emptyList(),
            cardClasses = ModelCatalog.TfLite.cardClasses.toSet(),
            lockOnThreshold = 5,
            memoryDetectionTimeLimit = 1200L
        )

        cardDetector.use { detector ->
            Log.d(
                "YoloCardDetector",
                "classId".padStart(3) + ",  " +
                    "confidence".padStart(10) + ",  " +
                    "id".padStart(4) + ",  " +
                    "lock".padStart(4) + ",  " +
                    "lockingStatus".padStart(12) + ",  " +
                    "dHash".padStart(22) + ", " +
                    "x".padStart(10) + ",  " +
                    "y".padStart(10) + ",  " +
                    "x2".padStart(10) + ",  " +
                    "y2".padStart(10)
            )

            frames.withIndex().forEach { (index, frame) ->
                val expectedBox = groundTruth.toPixelBox(frame.width, frame.height)

                val startTime = System.currentTimeMillis()
                val cardDetection = detector.track(frame)
                val detectionTime = System.currentTimeMillis() - startTime

                assertTrue("Expected at least one card detection for frame $index", cardDetection != null)

                if (cardDetection != null) {
                    val iou = calculateIoU(cardDetection.card.box, expectedBox)

                    Log.d(
                        "YoloCardDetector",
                        cardDetection.card.classId.toString().padStart(7) + ",  " +
                            "%.7f".format(cardDetection.card.confidence).padStart(10) + ",  " +
                            (cardDetection.id?.toString() ?: "null").padStart(4) + ",  " +
                            "%.1f".format(cardDetection.lockOnProgress).padStart(4) + ",  " +
                            cardDetection.lockingStatus.toString().padStart(13) + ",  " +
                            cardDetection.card.image.generateDHash().toString().padStart(22) + ", " +
                            "%.7f".format(cardDetection.card.box.x.toFloat() / frame.width).padStart(10) + ",  " +
                            "%.7f".format(cardDetection.card.box.y.toFloat() / frame.height).padStart(10) + ",  " +
                            "%.7f".format(cardDetection.card.box.x2.toFloat() / frame.width).padStart(10) + ",  " +
                            "%.7f".format(cardDetection.card.box.y2.toFloat() / frame.height).padStart(10)
                    )

                } else {
                    Log.d("YoloCardDetector", "Frame $index: No card detected")
                }
            }
        }
    }
}
