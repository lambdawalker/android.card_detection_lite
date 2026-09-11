package com.apexfission.android.carddetectionlite.resource

import android.graphics.Bitmap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify

class BitmapTransferTest {
    @Test
    fun takeCopyTransfersBitmapExactlyOnce() {
        val bitmap = mock(Bitmap::class.java)
        val transfer = OneShotBitmapTransfer(bitmap)

        assertSame(bitmap, transfer.takeCopy())
        transfer.expire()

        verify(bitmap, never()).recycle()
        val error = assertThrows(IllegalStateException::class.java) {
            transfer.takeCopy()
        }
        assertEquals(BitmapTransferException.AlreadyConsumed, error.message)
    }

    @Test
    fun unusedTransferRecyclesBitmapWhenCallbackExpires() {
        val bitmap = mock(Bitmap::class.java)
        val transfer = OneShotBitmapTransfer(bitmap)

        transfer.expire()

        verify(bitmap).recycle()
        val error = assertThrows(IllegalStateException::class.java) {
            transfer.takeCopy()
        }
        assertEquals(BitmapTransferException.Expired, error.message)
    }

    @Test
    fun concurrentCallersAllowOnlyOneOwnershipTransfer() {
        val bitmap = mock(Bitmap::class.java)
        val transfer = OneShotBitmapTransfer(bitmap)
        val ready = CountDownLatch(8)
        val start = CountDownLatch(1)
        val successes = AtomicInteger(0)
        val executor = Executors.newFixedThreadPool(8)

        repeat(8) {
            executor.execute {
                ready.countDown()
                start.await()
                try {
                    transfer.takeCopy()
                    successes.incrementAndGet()
                } catch (_: IllegalStateException) {
                    // Expected for every caller except the owner.
                }
            }
        }

        ready.await(2, TimeUnit.SECONDS)
        start.countDown()
        executor.shutdown()
        executor.awaitTermination(2, TimeUnit.SECONDS)

        assertEquals(1, successes.get())
        verify(bitmap, never()).recycle()
    }

    @Test
    fun bitmapUseRecyclesAfterSuccessfulBlock() {
        val bitmap = mock(Bitmap::class.java)

        bitmap.use { assertSame(bitmap, it) }

        verify(bitmap).recycle()
    }

    @Test
    fun bitmapUseRecyclesAfterFailedBlock() {
        val bitmap = mock(Bitmap::class.java)

        assertThrows(IllegalArgumentException::class.java) {
            bitmap.use { throw IllegalArgumentException("OCR failed") }
        }

        verify(bitmap).recycle()
    }
}
