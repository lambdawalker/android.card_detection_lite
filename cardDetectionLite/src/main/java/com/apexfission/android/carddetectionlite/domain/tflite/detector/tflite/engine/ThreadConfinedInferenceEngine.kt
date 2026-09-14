package com.apexfission.android.carddetectionlite.domain.tflite.detector.tflite.engine

import android.graphics.Bitmap
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.RejectedExecutionException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.runBlocking

/**
 * A wrapper for [InferenceEngine] that enforces thread affinity by ensuring initialization,
 * inference, and teardown all run on a single dedicated worker thread.
 */
class ThreadConfinedInferenceEngine private constructor(
    private val params: EngineInitParams
) : InferenceEngine by params.coreEngine {

    constructor(
        sharedDispatcher: CoroutineDispatcher? = null,
        engineFactory: () -> InferenceEngine
    ) : this(createInitParams(sharedDispatcher, engineFactory))

    constructor(
        engineFactory: () -> InferenceEngine
    ) : this(null, engineFactory)

    private var isClosed = false

    private val engineExecutor: ExecutorService? get() = params.engineExecutor
    private val engineDispatcher: CoroutineDispatcher get() = params.engineDispatcher

    internal val engineThreadId: Long get() = params.engineThreadId

    internal var lastInferenceThreadId: Long = -1L
        private set

    internal var closeThreadId: Long = -1L
        private set

    private class EngineInitParams(
        val coreEngine: InferenceEngine,
        val engineExecutor: ExecutorService?,
        val engineDispatcher: CoroutineDispatcher,
        val engineThreadId: Long,
    )

    companion object {
        private fun createInitParams(
            sharedDispatcher: CoroutineDispatcher?,
            engineFactory: () -> InferenceEngine
        ): EngineInitParams {
            val (executor, dispatcher) = if (sharedDispatcher != null) {
                null to sharedDispatcher
            } else {
                val exec = Executors.newSingleThreadExecutor { runnable ->
                    Thread(runnable, "TfliteEngineThread")
                }
                exec to exec.asCoroutineDispatcher()
            }

            var threadId = -1L
            val engine = runBlocking(dispatcher) {
                @Suppress("DEPRECATION")
                threadId = Thread.currentThread().id
                engineFactory()
            }
            return EngineInitParams(engine, executor, dispatcher, threadId)
        }
    }

    override fun runInference(bitmap: Bitmap): FloatArray {
        if (isClosed) return FloatArray(0)
        return try {
            runBlocking(engineDispatcher) {
                if (isClosed) return@runBlocking FloatArray(0)
                @Suppress("DEPRECATION")
                lastInferenceThreadId = Thread.currentThread().id
                params.coreEngine.runInference(bitmap)
            }
        } catch (_: RejectedExecutionException) {
            FloatArray(0)
        }
    }

    override fun close() {
        if (isClosed) return
        try {
            runBlocking(engineDispatcher) {
                if (isClosed) return@runBlocking
                isClosed = true
                @Suppress("DEPRECATION")
                closeThreadId = Thread.currentThread().id
                params.coreEngine.close()
            }
        } catch (_: RejectedExecutionException) {
            isClosed = true
        } finally {
            engineExecutor?.shutdown()
        }
    }
}
