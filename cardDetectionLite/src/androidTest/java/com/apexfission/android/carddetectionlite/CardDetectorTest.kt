package com.apexfission.android.carddetectionlite

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.apexfission.android.carddetectionlite.domain.ModelCatalog
import com.apexfission.android.carddetectionlite.domain.tflite.detector.CardDetector
import com.apexfission.android.carddetectionlite.domain.tflite.detector.YoloDetector
import com.apexfission.android.carddetectionlite.domain.tflite.image.generateDHash
import com.apexfission.android.carddetectionlite.domain.tflite.model.LockingStatus
import com.apexfission.android.carddetectionlite.tfmodel.cardClasses
import com.apexfission.android.carddetectionlite.tfmodel.modelPath
import com.apexfission.android.carddetectionlite.ui.detector.NumThreads
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests for [CardDetector] using test video asset `test/video/A/source.mp4`,
 * feature criteria `test/video/A/.feature`, and expected data `test/video/A/expected_data.txt`.
 */
@RunWith(AndroidJUnit4::class)
class CardDetectorTest {

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

    private fun createCardTracker(context: Context): CardDetector {
        val yoloDetector = YoloDetector(
            context = context,
            modelPath = ModelCatalog.TfLite.modelPath,
            scoreThreshold = 0.3f,
            iouThreshold = 0.45f,
            useGpu = false,
            numThreads = NumThreads.Default
        )

        return CardDetector(
            yoloDetector = yoloDetector,
            cardValidators = emptyList(),
            cardClasses = ModelCatalog.TfLite.cardClasses.toSet(),
            lockOnThreshold = 5,
            memoryDetectionTimeLimit = 1200L
        )
    }

    @Test
    fun testCardDetectionFromVideoFrames() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        val rawGroundTruth = context.assets.open("test/video/A/frame_0_expected_data.txt").bufferedReader().use {
            it.readText()
        }
        val groundTruth = parseGroundTruth(rawGroundTruth)

        val videoPath = "test/video/A/source.mp4"
        Log.d("CardTrackerTest", "Extracting frames from $videoPath")
        val frames = extractFramesFromVideo(context, videoPath, frameIntervalMs = 100L, maxFrames = 15)
        assertTrue("Expected to extract video frames from $videoPath", frames.isNotEmpty())

        Log.d("CardTrackerTest", "Loading Model")
        val cardDetector = createCardTracker(context)

        cardDetector.use { detector ->
            Log.d(
                "CardTrackerTest",
                "frame".padStart(6) + ",  " +
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

                val cardDetection = detector.track(frame)
                assertTrue("Expected at least one card detection for frame $index", cardDetection != null)

                if (cardDetection != null) {
                    val iou = calculateIoU(cardDetection.card.box, expectedBox)

                    Log.d(
                        "CardTrackerTest",
                        index.toString().padStart(6) + ",  " +
                            cardDetection.card.classId.toString().padStart(7) + ",  " +
                            "%.7f".format(cardDetection.card.confidence).padStart(10) + ",  " +
                            (cardDetection.id?.toString() ?: "null").padStart(4) + ",  " +
                            "%.1f".format(cardDetection.lockOnProgress).padStart(4) + ",  " +
                            cardDetection.lockingStatus.toString().padStart(13) + ",  " +
                            cardDetection.card.image.generateDHash().toString().padStart(22) + ", " +
                            "%.7f".format(cardDetection.card.box.x.toFloat() / frame.width).padStart(10) + ",  " +
                            "%.7f".format(cardDetection.card.box.y.toFloat() / frame.height).padStart(10) + ",  " +
                            "%.7f".format(cardDetection.card.box.x2.toFloat() / frame.width).padStart(10) + ",  " +
                            "%.7f".format(cardDetection.card.box.x2.toFloat() / frame.height).padStart(10)
                    )

                    assertTrue("IoU for frame $index should be >= 0.5, got $iou", iou >= 0.5f)
                }
            }
        }
    }

    /**
     * Verifies that tracking output on `source.mp4` matches all expected rows in `expected_data.txt`.
     */
    @Test
    fun testExpectedDataSetMatching() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        val rawExpectedText = context.assets.open("test/video/A/expected_data.txt").bufferedReader().use {
            it.readText()
        }
        val expectedFrames = parseExpectedData(rawExpectedText)
        assertTrue("Expected dataset should not be empty", expectedFrames.isNotEmpty())

        val frames = extractFramesFromVideo(
            context,
            "test/video/A/source.mp4",
            frameIntervalMs = 100L,
            maxFrames = expectedFrames.size
        )
        assertEquals("Frame count should match expected dataset size", expectedFrames.size, frames.size)

        val tracker = createCardTracker(context)

        tracker.use { detector ->
            for ((index, expected) in expectedFrames.withIndex()) {
                val frame = frames[index]
                val detection = detector.track(frame)

                assertNotNull("Expected valid detection for frame $index", detection)
                detection!!

                assertEquals("classId mismatch at frame $index", expected.classId, detection.card.classId)
                assertEquals("id mismatch at frame $index", expected.id, detection.id)
                assertEquals("lockOnProgress mismatch at frame $index", expected.lockProgress, detection.lockOnProgress, 0.01f)
                assertEquals("lockingStatus mismatch at frame $index", expected.lockingStatus, detection.lockingStatus)

                val expectedBox = expected.toPixelBox(frame.width, frame.height)
                val iou = calculateIoU(detection.card.box, expectedBox)
                assertTrue(
                    "IoU between detection box (${detection.card.box}) and expected box ($expectedBox) at frame $index should be >= 0.85, but was $iou",
                    iou >= 0.85f
                )
            }
        }
    }

    /**
     * Acceptance Criteria Test:
     * Verifies that card locking transitions occur precisely after 5 consecutive qualifying frames.
     *
     * Scenario:
     * - Frame 0..3: progress increments by 0.2 (0.2, 0.4, 0.6, 0.8), id is null, status is LockingCard
     * - Frame 4: progress reaches 1.0, id is assigned (1), status is NewCard
     * - Frame 5+: progress remains 1.0, id remains 1, status is CardLocked
     */
    @Test
    fun testCardLockingProgressionAcceptanceCriteria() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val frames = extractFramesFromVideo(context, "test/video/A/source.mp4", frameIntervalMs = 100L, maxFrames = 10)
        assertTrue("Expected at least 10 frames from video", frames.size >= 10)

        val tracker = createCardTracker(context)

        tracker.use { detector ->
            var previousProgress = 0f

            for ((index, frame) in frames.withIndex()) {
                val detection = detector.track(frame)
                assertNotNull("Expected detection on frame $index", detection)
                detection!!

                assertTrue("Lock progress must not exceed 1.0 at frame $index", detection.lockOnProgress <= 1.0f)
                assertTrue("Lock progress must not decrease during stable video at frame $index", detection.lockOnProgress >= previousProgress)
                previousProgress = detection.lockOnProgress

                when (index) {
                    in 0..3 -> {
                        val expectedProgress = (index + 1) * 0.2f
                        assertEquals("Progress at frame $index", expectedProgress, detection.lockOnProgress, 0.01f)
                        assertEquals("ID at frame $index should be null", null, detection.id)
                        assertEquals("Status at frame $index should be LockingCard", LockingStatus.LockingCard, detection.lockingStatus)
                    }
                    4 -> {
                        assertEquals("Progress at frame 4 should be 1.0", 1.0f, detection.lockOnProgress, 0.01f)
                        assertEquals("ID at frame 4 should be 1", 1L, detection.id)
                        assertEquals("Status at frame 4 should be NewCard", LockingStatus.NewCard, detection.lockingStatus)
                    }
                    else -> {
                        assertEquals("Progress at frame $index should stay 1.0", 1.0f, detection.lockOnProgress, 0.01f)
                        assertEquals("ID at frame $index should stay 1", 1L, detection.id)
                        assertEquals("Status at frame $index should stay CardLocked", LockingStatus.CardLocked, detection.lockingStatus)
                    }
                }
            }
        }
    }

    /**
     * Robustness Criteria Test:
     * Verifies that once a card is locked (frame 4, id = 1), minor visual/spatial fluctuations
     * across subsequent frames do not restart the locking sequence or change the assigned card id.
     */
    @Test
    fun testLockedIdentitySurvivesMinorFluctuations() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val frames = extractFramesFromVideo(context, "test/video/A/source.mp4", frameIntervalMs = 100L, maxFrames = 15)

        val tracker = createCardTracker(context)

        tracker.use { detector ->
            var lockedCardId: Long? = null

            for ((index, frame) in frames.withIndex()) {
                val detection = detector.track(frame)
                assertNotNull("Expected valid detection for frame $index", detection)
                detection!!

                if (index == 4) {
                    assertEquals("Frame 4 should lock new card", LockingStatus.NewCard, detection.lockingStatus)
                    lockedCardId = detection.id
                    assertNotNull("Locked card ID should be assigned on frame 4", lockedCardId)
                } else if (index > 4) {
                    assertEquals("Status should remain CardLocked for frame $index", LockingStatus.CardLocked, detection.lockingStatus)
                    assertEquals("Card ID should remain $lockedCardId for frame $index", lockedCardId, detection.id)
                    assertEquals("Lock progress should remain 1.0 for frame $index", 1.0f, detection.lockOnProgress, 0.01f)
                }
            }
        }
    }
}
