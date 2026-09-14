package com.apexfission.android.carddetectionlite.domain.tflite.detector.tflite.engine

import android.content.Context
import com.apexfission.android.carddetectionlite.ui.detector.NumThreads
import kotlinx.coroutines.CoroutineDispatcher

fun buildInferenceEngine(
    context: Context,
    modelPath: String,
    useGpu: Boolean,
    numThreads: NumThreads = NumThreads.Default,
    sharedDispatcher: CoroutineDispatcher? = null
): InferenceEngine = buildThreadConfinedInferenceEngine(context, modelPath, useGpu, numThreads, sharedDispatcher)

fun buildThreadConfinedInferenceEngine(
    context: Context,
    modelPath: String,
    useGpu: Boolean,
    numThreads: NumThreads = NumThreads.Default,
    sharedDispatcher: CoroutineDispatcher? = null
): InferenceEngine {
    return ThreadConfinedInferenceEngine(sharedDispatcher) {
        InferenceCore(context, modelPath, useGpu, numThreads)
    }
}
