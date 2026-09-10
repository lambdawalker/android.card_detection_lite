package com.apexfission.android.carddetectionlite.ui.detector

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.apexfission.android.carddetectionlite.domain.tflite.detector.PreProcessingImageTransformation
import com.apexfission.android.carddetectionlite.domain.tflite.filters.CardValidator

/**
 * A [ViewModelProvider.Factory] responsible for creating instances of [CardDetectorLiteViewModel].
 */
class CardDetectorLiteViewModelFactory(
    private val application: Application,
    private val modelPath: String,
    private val classLabels: Map<Int, String> = emptyMap(),
    private val useGpu: Boolean,
    private val scoreThreshold: Float,
    private val iouThreshold: Float,
    private val cardFilters: List<CardValidator>,
    private val cardClasses: Set<Int>,
    private val inferenceIntervalMs: Long,
    private val lockOnThreshold: Int,
    private val noDetectionCountLimit: Int,
    private val memoryDetectionTimeLimit: Long,
    private val validateClassIdInLockOnProcess: Boolean,
    private val differenceHashDistanceLimit: Int,
    private val allowTemporalDrift: Boolean,
    private val preProcessingImageTransformation: PreProcessingImageTransformation,
    private val numThreads: NumThreads,
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CardDetectorLiteViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CardDetectorLiteViewModel(
                application = application,
                modelPath = modelPath,
                classLabels = classLabels,
                cardClasses = cardClasses,
                useGpu = useGpu,
                scoreThreshold = scoreThreshold,
                iouThreshold = iouThreshold,
                cardFilters = cardFilters,
                inferenceIntervalMs = inferenceIntervalMs,
                lockOnThreshold = lockOnThreshold,
                noDetectionCountLimit = noDetectionCountLimit,
                memoryDetectionTimeLimit = memoryDetectionTimeLimit,
                validateClassIdInLockOnProcess = validateClassIdInLockOnProcess,
                differenceHashDistanceLimit = differenceHashDistanceLimit,
                allowTemporalDrift = allowTemporalDrift,
                preProcessingImageTransformation = preProcessingImageTransformation,
                numThreads = numThreads
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
