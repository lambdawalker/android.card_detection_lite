package com.apexfission.android.carddetectionlite.ui.detector

import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SingleFlightGateTest {
    @Test
    fun rejectsSecondAcquisitionUntilReleased() {
        val gate = SingleFlightGate()

        assertTrue(gate.tryAcquire())
        assertFalse(gate.tryAcquire())

        gate.release()

        assertTrue(gate.tryAcquire())
        gate.release()
    }

    @Test
    fun concurrentCallersAllowOnlyOneAcquisition() {
        val gate = SingleFlightGate()
        val ready = CountDownLatch(16)
        val start = CountDownLatch(1)
        val acquired = AtomicInteger(0)
        val executor = Executors.newFixedThreadPool(16)

        repeat(16) {
            executor.execute {
                ready.countDown()
                start.await()
                if (gate.tryAcquire()) acquired.incrementAndGet()
            }
        }

        assertTrue(ready.await(2, TimeUnit.SECONDS))
        start.countDown()
        executor.shutdown()
        assertTrue(executor.awaitTermination(2, TimeUnit.SECONDS))
        assertEquals(1, acquired.get())

        gate.release()
    }
}
