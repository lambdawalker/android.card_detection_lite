package com.apexfission.android.carddetectionlite.domain.tflite.detector.tflite.engine

import java.io.Closeable
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher

/**
 * Provides a shared single-thread executor and dispatcher for orchestrating TFLite inference,
 * YOLO detection, and card tracking on a single worker thread.
 */
class EngineThreadDispatcher(
    val executor: ExecutorService = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "SingleThreadEngineWorker")
    }
) : Closeable {

    val dispatcher: CoroutineDispatcher = executor.asCoroutineDispatcher()

    override fun close() {
        if (!executor.isShutdown) {
            executor.shutdown()
        }
    }
}
