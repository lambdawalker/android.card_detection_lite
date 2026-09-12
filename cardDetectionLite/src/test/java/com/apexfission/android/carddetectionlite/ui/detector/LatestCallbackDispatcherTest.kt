package com.apexfission.android.carddetectionlite.ui.detector

import java.util.Collections
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LatestCallbackDispatcherTest {

    @Test
    fun `slow callback keeps only latest pending value and does not block submitter`() {
        val executor = Executors.newSingleThreadExecutor()
        val dispatcher = executor.asCoroutineDispatcher()
        val scope = CoroutineScope(SupervisorJob())
        val released = Collections.synchronizedList(mutableListOf<Int>())
        val delivered = Collections.synchronizedList(mutableListOf<Int>())
        val firstStarted = CountDownLatch(1)
        val releaseFirst = CountDownLatch(1)
        val latestDelivered = CountDownLatch(1)
        val callbackDispatcher = LatestCallbackDispatcher<Int>(
            scope = scope,
            dispatcher = dispatcher,
            releaseUndelivered = released::add,
        )

        try {
            callbackDispatcher.submit(1) {
                delivered += it
                firstStarted.countDown()
                releaseFirst.await(2, TimeUnit.SECONDS)
            }
            assertTrue(firstStarted.await(2, TimeUnit.SECONDS))

            callbackDispatcher.submit(2) { delivered += it }
            callbackDispatcher.submit(3) {
                delivered += it
                latestDelivered.countDown()
            }

            assertEquals(listOf(2), released.toList())
            releaseFirst.countDown()
            assertTrue(latestDelivered.await(2, TimeUnit.SECONDS))
            assertEquals(listOf(1, 3), delivered.toList())
        } finally {
            callbackDispatcher.close()
            scope.cancel()
            dispatcher.close()
            executor.shutdownNow()
        }
    }

    @Test
    fun `close releases a pending value that was never handed to its callback`() {
        val executor = Executors.newSingleThreadExecutor()
        val dispatcher = executor.asCoroutineDispatcher()
        val scope = CoroutineScope(SupervisorJob())
        val released = Collections.synchronizedList(mutableListOf<Int>())
        val firstStarted = CountDownLatch(1)
        val releaseFirst = CountDownLatch(1)
        val callbackDispatcher = LatestCallbackDispatcher<Int>(
            scope = scope,
            dispatcher = dispatcher,
            releaseUndelivered = released::add,
        )

        try {
            callbackDispatcher.submit(1) {
                firstStarted.countDown()
                releaseFirst.await(2, TimeUnit.SECONDS)
            }
            assertTrue(firstStarted.await(2, TimeUnit.SECONDS))
            callbackDispatcher.submit(2) {}

            callbackDispatcher.close()
            assertEquals(listOf(2), released.toList())
        } finally {
            releaseFirst.countDown()
            scope.cancel()
            dispatcher.close()
            executor.shutdownNow()
        }
    }

    @Test
    fun `callback failure does not stop later deliveries`() {
        val executor = Executors.newSingleThreadExecutor()
        val dispatcher = executor.asCoroutineDispatcher()
        val scope = CoroutineScope(SupervisorJob())
        val failures = Collections.synchronizedList(mutableListOf<Throwable>())
        val failureObserved = CountDownLatch(1)
        val delivered = CountDownLatch(1)
        val callbackDispatcher = LatestCallbackDispatcher<Int>(
            scope = scope,
            dispatcher = dispatcher,
            releaseUndelivered = {},
            onCallbackFailure = {
                failures += it
                failureObserved.countDown()
            },
        )

        try {
            callbackDispatcher.submit(1) { error("consumer failure") }
            assertTrue(failureObserved.await(2, TimeUnit.SECONDS))
            callbackDispatcher.submit(2) { delivered.countDown() }

            assertTrue(delivered.await(2, TimeUnit.SECONDS))
            assertEquals(1, failures.size)
            assertEquals("consumer failure", failures.single().message)
        } finally {
            callbackDispatcher.close()
            scope.cancel()
            dispatcher.close()
            executor.shutdownNow()
        }
    }

    @Test
    fun `submission after close is released`() {
        val executor = Executors.newSingleThreadExecutor()
        val dispatcher = executor.asCoroutineDispatcher()
        val scope = CoroutineScope(SupervisorJob())
        val released = mutableListOf<Int>()
        val callbackDispatcher = LatestCallbackDispatcher<Int>(
            scope = scope,
            dispatcher = dispatcher,
            releaseUndelivered = released::add,
        )

        callbackDispatcher.close()
        callbackDispatcher.submit(1) {}

        assertEquals(listOf(1), released)
        scope.cancel()
        dispatcher.close()
        executor.shutdownNow()
    }
}
