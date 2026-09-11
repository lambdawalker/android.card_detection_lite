package com.apexfission.android.carddetectionlite.ui.simulation

import android.graphics.Bitmap
import java.util.concurrent.Executor
import java.util.concurrent.RejectedExecutionException
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify

class BitmapCallbackDispatchTest {
    @Test
    fun successfulDispatchTransfersOwnershipWithoutRecycling() {
        val bitmap = mock(Bitmap::class.java)
        var delivered: Bitmap? = null
        val directExecutor = Executor { it.run() }

        directExecutor.executeTransferring(bitmap) { delivered = it }

        assertSame(bitmap, delivered)
        verify(bitmap, never()).recycle()
    }

    @Test
    fun rejectedDispatchRecyclesBitmap() {
        val bitmap = mock(Bitmap::class.java)
        val rejectingExecutor = Executor { throw RejectedExecutionException("stopped") }

        assertThrows(RejectedExecutionException::class.java) {
            rejectingExecutor.executeTransferring(bitmap) { }
        }

        verify(bitmap).recycle()
    }

    @Test
    fun callbackFailureDoesNotReclaimTransferredBitmap() {
        val bitmap = mock(Bitmap::class.java)
        val directExecutor = Executor { it.run() }

        assertThrows(IllegalStateException::class.java) {
            directExecutor.executeTransferring(bitmap) {
                throw IllegalStateException("receiver failed")
            }
        }

        verify(bitmap, never()).recycle()
    }
}
