package com.apexfission.android.carddetectionlite.domain.tflite.detector.yolo.engine

import android.content.Context
import com.apexfission.android.carddetectionlite.ui.detector.NumThreads
import kotlinx.coroutines.CoroutineDispatcher

fun buildYoloDetector(
    context: Context,
    modelPath: String,
    scoreThreshold: Float,
    iouThreshold: Float,
    useGpu: Boolean,
    maxNmsCandidates: Int = 150,
    numThreads: NumThreads = NumThreads.Default,
    sharedDispatcher: CoroutineDispatcher? = null
): Detector = buildThreadConfinedYoloDetector(
    context = context,
    modelPath = modelPath,
    scoreThreshold = scoreThreshold,
    iouThreshold = iouThreshold,
    useGpu = useGpu,
    maxNmsCandidates = maxNmsCandidates,
    numThreads = numThreads,
    sharedDispatcher = sharedDispatcher
)

fun buildThreadConfinedYoloDetector(
    context: Context,
    modelPath: String,
    scoreThreshold: Float,
    iouThreshold: Float,
    useGpu: Boolean,
    maxNmsCandidates: Int = 150,
    numThreads: NumThreads = NumThreads.Default,
    sharedDispatcher: CoroutineDispatcher? = null
): Detector {
    return ThreadConfinedYoloDetector(sharedDispatcher) {
        YoloDetector(
            context = context,
            modelPath = modelPath,
            scoreThreshold = scoreThreshold,
            iouThreshold = iouThreshold,
            useGpu = useGpu,
            maxNmsCandidates = maxNmsCandidates,
            numThreads = numThreads
        )
    }
}
