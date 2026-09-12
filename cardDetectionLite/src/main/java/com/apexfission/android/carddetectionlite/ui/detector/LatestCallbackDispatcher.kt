package com.apexfission.android.carddetectionlite.ui.detector

import java.io.Closeable
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

/**
 * Delivers callbacks serially without blocking the producer.
 *
 * One callback may be running and one value may be pending. A newer value replaces an older
 * pending value, which is released before ownership ever reaches its callback.
 */
internal class LatestCallbackDispatcher<T>(
    scope: CoroutineScope,
    dispatcher: CoroutineDispatcher,
    private val releaseUndelivered: (T) -> Unit,
    private val onCallbackFailure: (Throwable) -> Unit = {},
) : Closeable {
    private data class Delivery<T>(
        val value: T,
        val callback: (T) -> Unit,
    )

    private val deliveries = Channel<Delivery<T>>(
        capacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
        onUndeliveredElement = { delivery: Delivery<T> ->
            releaseUndelivered(delivery.value)
        },
    )

    private val worker: Job = scope.launch(dispatcher) {
        for (delivery in deliveries) {
            try {
                // Ownership transfers immediately before entering application code.
                delivery.callback(delivery.value)
            } catch (throwable: Throwable) {
                onCallbackFailure(throwable)
            }
        }
    }

    fun submit(value: T, callback: (T) -> Unit) {
        val result = deliveries.trySend(Delivery(value, callback))
        if (result.isFailure) {
            releaseUndelivered(value)
        }
    }

    override fun close() {
        worker.cancel()
        deliveries.cancel()
    }
}
