package com.apexfission.android.carddetectionlite.ui.simulation

import java.io.Closeable
import java.util.concurrent.Executor

/**
 * Tracks values queued on an [Executor] until their callback actually begins.
 *
 * Closing releases all pending values immediately. A callback that has already begun owns its
 * value, and [close] waits for that ownership handoff to finish before returning.
 */
internal class ReleaseAwareCallbackDispatcher<T>(
    private val executor: Executor,
    private val releaseUndelivered: (T) -> Unit,
) : Closeable {
    private class Pending<T>(val value: T)

    private val lock = Any()
    private val pending = LinkedHashSet<Pending<T>>()
    private var closed = false

    fun submit(value: T, callback: (T) -> Unit) {
        val delivery = Pending(value)
        val accepted = synchronized(lock) {
            if (closed) false else pending.add(delivery)
        }

        if (!accepted) {
            releaseUndelivered(value)
            return
        }

        try {
            executor.execute {
                synchronized(lock) {
                    if (pending.remove(delivery)) {
                        // Keep the lifecycle lock through invocation so close() cannot return while
                        // a callback is between the ownership decision and application code.
                        callback(delivery.value)
                    }
                }
            }
        } catch (throwable: Throwable) {
            val stillOwned = synchronized(lock) { pending.remove(delivery) }
            if (stillOwned) releaseUndelivered(value)
            throw throwable
        }
    }

    override fun close() {
        val undelivered = synchronized(lock) {
            if (closed) return
            closed = true
            pending.toList().also { pending.clear() }
        }
        undelivered.forEach { releaseUndelivered(it.value) }
    }
}
