package com.apexfission.android.carddetectionlite.domain.tflite.detector.card.engine

import android.graphics.Bitmap
import androidx.camera.core.ImageProxy
import com.apexfission.android.carddetectionlite.domain.tflite.model.CardDetection
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.RejectedExecutionException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.runBlocking

/**
 * A wrapper for [CardDetector] that confines initialization, tracking, and teardown
 * to a single dedicated worker thread or shared dispatcher.
 */
class ThreadConfinedCardDetector private constructor(
    private val params: DetectorInitParams
) : CardDetector by params.coreDetector {

    constructor(
        sharedDispatcher: CoroutineDispatcher? = null,
        detectorFactory: () -> CardDetector
    ) : this(createInitParams(sharedDispatcher, detectorFactory))

    constructor(
        detectorFactory: () -> CardDetector
    ) : this(null, detectorFactory)

    private var isClosed = false

    private val detectorExecutor: ExecutorService? get() = params.detectorExecutor
    private val detectorDispatcher: CoroutineDispatcher get() = params.detectorDispatcher

    internal val detectorThreadId: Long get() = params.detectorThreadId

    internal var lastTrackThreadId: Long = -1L
        private set

    internal var closeThreadId: Long = -1L
        private set

    private class DetectorInitParams(
        val coreDetector: CardDetector,
        val detectorExecutor: ExecutorService?,
        val detectorDispatcher: CoroutineDispatcher,
        val detectorThreadId: Long,
    )

    companion object {
        private fun createInitParams(
            sharedDispatcher: CoroutineDispatcher?,
            detectorFactory: () -> CardDetector
        ): DetectorInitParams {
            val (executor, dispatcher) = if (sharedDispatcher != null) {
                null to sharedDispatcher
            } else {
                val exec = Executors.newSingleThreadExecutor { runnable ->
                    Thread(runnable, "CardDetectorThread")
                }
                exec to exec.asCoroutineDispatcher()
            }

            var threadId = -1L
            val detector = runBlocking(dispatcher) {
                @Suppress("DEPRECATION")
                threadId = Thread.currentThread().id
                detectorFactory()
            }
            return DetectorInitParams(detector, executor, dispatcher, threadId)
        }
    }

    override fun track(imageProxy: ImageProxy): CardDetection? {
        if (isClosed) return null
        return try {
            runBlocking(detectorDispatcher) {
                if (isClosed) return@runBlocking null
                @Suppress("DEPRECATION")
                lastTrackThreadId = Thread.currentThread().id
                params.coreDetector.track(imageProxy)
            }
        } catch (_: RejectedExecutionException) {
            null
        }
    }

    override fun track(bitmap: Bitmap): CardDetection? {
        if (isClosed) return null
        return try {
            runBlocking(detectorDispatcher) {
                if (isClosed) return@runBlocking null
                @Suppress("DEPRECATION")
                lastTrackThreadId = Thread.currentThread().id
                params.coreDetector.track(bitmap)
            }
        } catch (_: RejectedExecutionException) {
            null
        }
    }

    override fun close() {
        if (isClosed) return
        try {
            runBlocking(detectorDispatcher) {
                if (isClosed) return@runBlocking
                isClosed = true
                @Suppress("DEPRECATION")
                closeThreadId = Thread.currentThread().id
                params.coreDetector.close()
            }
        } catch (_: RejectedExecutionException) {
            isClosed = true
        } finally {
            detectorExecutor?.shutdown()
        }
    }
}
