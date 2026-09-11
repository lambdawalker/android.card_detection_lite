package com.apexfission.android.carddetectionlite.ui.detector

import android.graphics.Bitmap
import com.apexfission.android.carddetectionlite.domain.coordinates.models.ImageBox
import com.apexfission.android.carddetectionlite.domain.tflite.model.CardDetection
import com.apexfission.android.carddetectionlite.domain.tflite.model.Feature
import com.apexfission.android.carddetectionlite.domain.tflite.model.LockingStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.kotlin.whenever

class LatestBestDetectionStoreTest {
    private fun detection(
        confidence: Float,
        id: Long? = 1L,
        status: LockingStatus = LockingStatus.CardLocked,
    ) = CardDetection(
        lockOnProgress = 1f,
        id = id,
        card = Feature(
            box = ImageBox.from2P(0u, 0u, 10u, 10u),
            confidence = confidence,
            classId = 0,
        ),
        features = emptyList(),
        lockingStatus = status,
    )

    @Test
    fun retainsOnlyHigherConfidenceImageForSameCard() {
        val store = LatestBestDetectionStore()
        val firstSource = sourceWithCopy()
        val worseSource = sourceWithCopy()
        val betterSource = sourceWithCopy()

        assertTrue(store.offer(detection(0.8f), firstSource.source))
        assertFalse(store.offer(detection(0.7f), worseSource.source))
        assertTrue(store.offer(detection(0.9f), betterSource.source))

        verify(worseSource.source, never()).copy(Bitmap.Config.ARGB_8888, false)
        verify(firstSource.copy).recycle()
        verify(betterSource.copy, never()).recycle()
        assertEquals(0.9f, store.detection()?.card?.confidence)
    }

    @Test
    fun newLockedCardReplacesPreviousBestRegardlessOfConfidence() {
        val store = LatestBestDetectionStore()
        val first = sourceWithCopy()
        val next = sourceWithCopy()

        store.offer(detection(0.95f, id = 1L), first.source)

        assertTrue(
            store.offer(
                detection(0.75f, id = 2L, status = LockingStatus.NewCard),
                next.source,
            )
        )
        verify(first.copy).recycle()
        assertEquals(2L, store.detection()?.id)
    }

    @Test
    fun captureReturnsIndependentOneShotTransfer() {
        val store = LatestBestDetectionStore()
        val retained = sourceWithCopy()
        val captured = mock(Bitmap::class.java)
        whenever(retained.copy.copy(Bitmap.Config.ARGB_8888, false)).thenReturn(captured)
        val expected = detection(0.9f)
        store.offer(expected, retained.source)

        var actualBitmap: Bitmap? = null
        val didCapture = store.withTransfer { actualDetection, transfer ->
            assertSame(expected, actualDetection)
            actualBitmap = transfer.takeCopy()
        }

        assertTrue(didCapture)
        assertSame(captured, actualBitmap)
        verify(captured, never()).recycle()
        verify(retained.copy, never()).recycle()
    }

    private data class BitmapPair(val source: Bitmap, val copy: Bitmap)

    private fun sourceWithCopy(): BitmapPair {
        val source = mock(Bitmap::class.java)
        val copy = mock(Bitmap::class.java)
        whenever(source.config).thenReturn(Bitmap.Config.ARGB_8888)
        whenever(source.copy(Bitmap.Config.ARGB_8888, false)).thenReturn(copy)
        whenever(copy.config).thenReturn(Bitmap.Config.ARGB_8888)
        return BitmapPair(source, copy)
    }
}
