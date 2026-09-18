package com.apexfission.android.carddetectionlite.domain.tflite.detector.card.engine

import android.graphics.Bitmap
import android.graphics.Color
import com.apexfission.android.math.models.ImageBox
import com.apexfission.android.carddetectionlite.domain.tflite.detector.yolo.engine.Detector
import com.apexfission.android.carddetectionlite.domain.tflite.model.Detection
import com.apexfission.android.carddetectionlite.domain.tflite.model.DetectionSource
import com.apexfission.android.carddetectionlite.domain.tflite.model.LockingStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class DefaultCardDetectorTest {

    private fun createSolidBitmap(width: Int, height: Int, color: Int): Bitmap {
        val bitmap = mock<Bitmap>()
        whenever(bitmap.width).thenReturn(width)
        whenever(bitmap.height).thenReturn(height)
        whenever(bitmap.getPixel(any(), any())).thenReturn(color)
        return bitmap
    }

    private fun createPatternedBitmap(width: Int, height: Int): Bitmap {
        val bitmap = mock<Bitmap>()
        whenever(bitmap.width).thenReturn(width)
        whenever(bitmap.height).thenReturn(height)
        whenever(bitmap.getPixel(any(), any())).thenAnswer { invocation ->
            val x = invocation.getArgument<Int>(0)
            val y = invocation.getArgument<Int>(1)
            if ((x + y) % 2 == 0) Color.WHITE else Color.BLACK
        }
        return bitmap
    }

    @Test
    fun `first frame calls yolo detector and subsequent frames use dhash without calling yolo`() {
        val yoloDetector = mock<Detector>()
        val box = ImageBox.from2P(10, 10, 110, 80)
        val initialDetection = Detection(box, 0.9f, 0)
        val bitmap = createPatternedBitmap(200, 200)

        whenever(yoloDetector.detect(any<Bitmap>())).thenReturn(listOf(initialDetection))

        val cardDetector = DefaultCardDetector(
            yoloDetector = yoloDetector,
            cardValidators = emptyList(),
            cardClasses = setOf(0),
            lockOnThreshold = 3,
            hashBasedSearchFrameLimit = 2
        )

        // Frame 1: count=0, no previous detection -> calls yolo
        val r1 = cardDetector.track(bitmap)
        assertNotNull(r1)
        assertEquals(LockingStatus.LockingCard, r1?.lockingStatus)
        assertEquals(DetectionSource.Yolo, r1?.detectionSource)
        verify(yoloDetector, times(1)).detect(bitmap)

        // Frame 2: count=0 < hashBasedSearchFrameLimit(2), uses dHash match -> does not call yolo
        val r2 = cardDetector.track(bitmap)
        assertNotNull(r2)
        assertEquals(LockingStatus.LockingCard, r2?.lockingStatus)
        assertEquals(DetectionSource.Hash, r2?.detectionSource)
        verify(yoloDetector, times(1)).detect(bitmap) // count is still 1

        // Frame 3: count=1 < hashBasedSearchFrameLimit(2), uses dHash match -> does not call yolo
        val r3 = cardDetector.track(bitmap)
        assertNotNull(r3)
        assertEquals(LockingStatus.NewCard, r3?.lockingStatus)
        assertEquals(DetectionSource.Hash, r3?.detectionSource)
        verify(yoloDetector, times(1)).detect(bitmap) // count is still 1

        // Frame 4: count=2 >= hashBasedSearchFrameLimit(2) -> must call yolo again
        val r4 = cardDetector.track(bitmap)
        assertNotNull(r4)
        assertEquals(DetectionSource.Yolo, r4?.detectionSource)
        verify(yoloDetector, times(2)).detect(bitmap)
    }

    @Test
    fun `when dhash does not match it falls back to yolo detector`() {
        val yoloDetector = mock<Detector>()
        val box = ImageBox.from2P(10, 10, 110, 80)
        val detection = Detection(box, 0.9f, 0)
        val bitmap1 = createPatternedBitmap(200, 200)
        val bitmap2 = createSolidBitmap(200, 200, Color.RED)

        whenever(yoloDetector.detect(bitmap1)).thenReturn(listOf(detection))
        whenever(yoloDetector.detect(bitmap2)).thenReturn(listOf(detection))

        val cardDetector = DefaultCardDetector(
            yoloDetector = yoloDetector,
            cardValidators = emptyList(),
            cardClasses = setOf(0),
            lockOnThreshold = 3,
            differenceHashDistanceLimit = 5,
            hashBasedSearchFrameLimit = 5
        )

        // Frame 1 with bitmap1
        val r1 = cardDetector.track(bitmap1)
        assertNotNull(r1)
        verify(yoloDetector, times(1)).detect(bitmap1)

        // Frame 2 with visually different bitmap2: dHash fails -> falls back to YOLO
        val r2 = cardDetector.track(bitmap2)
        assertNotNull(r2)
        verify(yoloDetector, times(1)).detect(bitmap2)
    }

    @Test
    fun `when no candidate is detected returns null and handles missing detection`() {
        val yoloDetector = mock<Detector>()
        val bitmap = createSolidBitmap(200, 200, Color.BLACK)
        whenever(yoloDetector.detect(any<Bitmap>())).thenReturn(emptyList())

        val cardDetector = DefaultCardDetector(
            yoloDetector = yoloDetector,
            cardValidators = emptyList(),
            cardClasses = setOf(0)
        )

        val result = cardDetector.track(bitmap)
        assertNull(result)
        verify(yoloDetector, times(1)).detect(bitmap)
    }
}
