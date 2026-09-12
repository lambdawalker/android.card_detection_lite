package com.apexfission.android.carddetectionlite.ui.simulation

import java.util.ArrayDeque
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executor
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class ReleaseAwareCallbackDispatcherTest {

    @Test
    fun `close immediately releases queued values and suppresses their callbacks`() {
        val executor = ManualExecutor()
        val released = mutableListOf<Int>()
        val delivered = mutableListOf<Int>()
        val dispatcher = ReleaseAwareCallbackDispatcher(executor, released::add)

        dispatcher.submit(1, delivered::add)
        dispatcher.submit(2, delivered::add)
        dispatcher.close()

        assertEquals(listOf(1, 2), released)
        executor.runAll()
        assertTrue(delivered.isEmpty())
        assertEquals(listOf(1, 2), released)
    }

    @Test
    fun `completed callback transfers ownership and is not released`() {
        val executor = ManualExecutor()
        val released = mutableListOf<Int>()
        val delivered = mutableListOf<Int>()
        val dispatcher = ReleaseAwareCallbackDispatcher(executor, released::add)

        dispatcher.submit(1, delivered::add)
        executor.runAll()
        dispatcher.close()

        assertEquals(listOf(1), delivered)
        assertTrue(released.isEmpty())
    }

    @Test
    fun `submission after close is released without reaching executor`() {
        val executor = ManualExecutor()
        val released = mutableListOf<Int>()
        val dispatcher = ReleaseAwareCallbackDispatcher(executor, released::add)
        dispatcher.close()

        dispatcher.submit(1) { error("must not run") }

        assertEquals(listOf(1), released)
        assertFalse(executor.hasTasks())
    }

    @Test
    fun `executor rejection releases value`() {
        val released = mutableListOf<Int>()
        val rejectingExecutor = Executor { throw RejectedExecutionException("closed") }
        val dispatcher = ReleaseAwareCallbackDispatcher(rejectingExecutor, released::add)

        assertThrows(RejectedExecutionException::class.java) {
            dispatcher.submit(1) { error("must not run") }
        }

        assertEquals(listOf(1), released)
    }

    @Test
    fun `close waits for callback already transferring ownership`() {
        val executor = Executor { command -> Thread(command).start() }
        val callbackStarted = CountDownLatch(1)
        val finishCallback = CountDownLatch(1)
        val closeReturned = CountDownLatch(1)
        val dispatcher = ReleaseAwareCallbackDispatcher<Int>(executor) {}

        dispatcher.submit(1) {
            callbackStarted.countDown()
            finishCallback.await(2, TimeUnit.SECONDS)
        }
        assertTrue(callbackStarted.await(2, TimeUnit.SECONDS))

        Thread {
            dispatcher.close()
            closeReturned.countDown()
        }.start()

        assertFalse(closeReturned.await(100, TimeUnit.MILLISECONDS))
        finishCallback.countDown()
        assertTrue(closeReturned.await(2, TimeUnit.SECONDS))
    }

    private class ManualExecutor : Executor {
        private val tasks = ArrayDeque<Runnable>()

        override fun execute(command: Runnable) {
            tasks.addLast(command)
        }

        fun runAll() {
            while (tasks.isNotEmpty()) tasks.removeFirst().run()
        }

        fun hasTasks(): Boolean = tasks.isNotEmpty()
    }
}
