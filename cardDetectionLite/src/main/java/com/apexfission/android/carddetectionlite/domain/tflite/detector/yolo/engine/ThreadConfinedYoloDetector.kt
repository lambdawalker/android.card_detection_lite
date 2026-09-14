package com.apexfission.android.carddetectionlite.domain.tflite.detector.yolo.engine

import android.graphics.Bitmap
import androidx.camera.core.ImageProxy
import com.apexfission.android.carddetectionlite.domain.tflite.model.Detection
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.RejectedExecutionException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.runBlocking

/**
 * A wrapper for [Detector] that confines initialization, detection, and teardown
 * to a single dedicated worker thread or shared dispatcher.
 */
class ThreadConfinedYoloDetector private constructor(
    private val params: DetectorInitParams
) : Detector by params.coreDetector {

    constructor(
        sharedDispatcher: CoroutineDispatcher? = null,
        detectorFactory: () -> Detector
    ) : this(createInitParams(sharedDispatcher, detectorFactory))

    constructor(
        detectorFactory: () -> Detector
    ) : this(null, detectorFactory)

    private var isClosed = false

    private val detectorExecutor: ExecutorService? get() = params.detectorExecutor
    private val detectorDispatcher: CoroutineDispatcher get() = params.detectorDispatcher

    internal val detectorThreadId: Long get() = params.detectorThreadId

    internal var lastDetectThreadId: Long = -1L
        private set

    internal var closeThreadId: Long = -1L
        private set

    private class DetectorInitParams(
        val coreDetector: Detector,
        val detectorExecutor: ExecutorService?,
        val detectorDispatcher: CoroutineDispatcher,
        val detectorThreadId: Long,
    )

    companion object {
        private fun createInitParams(
            sharedDispatcher: CoroutineDispatcher?,
            detectorFactory: () -> Detector
        ): DetectorInitParams {
            val (executor, dispatcher) = if (sharedDispatcher != null) {
                null to sharedDispatcher
            } else {
                val exec = Executors.newSingleThreadExecutor { runnable ->
                    Thread(runnable, "YoloDetectorThread")
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

    override fun detect(bitmap: Bitmap): List<Detection> {
        if (isClosed) return emptyList()
        return try {
            runBlocking(detectorDispatcher) {
                if (isClosed) return@runBlocking emptyList()
                @Suppress("DEPRECATION")
                lastDetectThreadId = Thread.currentThread().id
                params.coreDetector.detect(bitmap)
            }
        } catch (_: RejectedExecutionException) {
            emptyList()
        }
    }

    override fun detect(imageProxy: ImageProxy): List<Detection> {
        if (isClosed) return emptyList()
        return try {
            runBlocking(detectorDispatcher) {
                if (isClosed) return@runBlocking emptyList()
                @Suppress("DEPRECATION")
                lastDetectThreadId = Thread.currentThread().id
                params.coreDetector.detect(imageProxy)
            }
        } catch (_: RejectedExecutionException) {
            emptyList()
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
