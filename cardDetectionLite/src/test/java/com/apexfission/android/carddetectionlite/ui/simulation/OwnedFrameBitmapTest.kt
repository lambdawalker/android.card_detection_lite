package com.apexfission.android.carddetectionlite.ui.simulation

import android.graphics.Bitmap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify

class OwnedFrameBitmapTest {
    @Test
    fun recycleReleasesBitmapExactlyOnce() {
        val bitmap = mock(Bitmap::class.java)
        val frame = OwnedFrameBitmap(bitmap)

        frame.recycle()
        frame.recycle()

        verify(bitmap, times(1)).recycle()
    }

    @Test
    fun concurrentRecycleReleasesBitmapExactlyOnce() {
        val bitmap = mock(Bitmap::class.java)
        val frame = OwnedFrameBitmap(bitmap)
        val ready = CountDownLatch(8)
        val start = CountDownLatch(1)
        val executor = Executors.newFixedThreadPool(8)

        repeat(8) {
            executor.execute {
                ready.countDown()
                start.await()
                frame.recycle()
            }
        }

        ready.await(2, TimeUnit.SECONDS)
        start.countDown()
        executor.shutdown()
        executor.awaitTermination(2, TimeUnit.SECONDS)

        verify(bitmap, times(1)).recycle()
    }
}
