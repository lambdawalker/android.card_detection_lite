package com.apexfission.android.carddetectionlite.ui.camerapreview

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.apexfission.android.carddetectionlite.domain.tflite.filters.CardValidator
import com.apexfission.android.carddetectionlite.ui.NumThreads

/**
 * A [ViewModelProvider.Factory] responsible for creating instances of [CardDetectorLiteViewModel].
 *
 * @property application The application instance, needed by `AndroidViewModel`.
 * @property modelPath The asset path for the TFLite model.
 * @property useGpu A flag to enable or disable the GPU delegate.
 * @property scoreThreshold The minimum confidence for raw detections.
 * @property cardFilters A list of custom [CardValidator]s.
 * @property cardClasses The list of class IDs to be treated as primary card targets.
 * @property inferenceIntervalMs The minimum interval, in milliseconds, between consecutive inferences.
 * @property lockOnThreshold The number of consecutive frames a card must be detected and visually
 *                         similar before it is considered "locked on."
 * @property noDetectionCountLimit Number of consecutive missing detections allowed before reset.
 * @property numThreads The number of threads to use for inference on the CPU.
 */
class CardDetectorLiteViewModelFactory(
    private val application: Application,
    private val modelPath: String,
    private val useGpu: Boolean,
    private val scoreThreshold: Float,
    private val cardFilters: List<CardValidator>,
    private val cardClasses: Set<Int>,
    private val inferenceIntervalMs: Long,
    private val lockOnThreshold: Int,
    private val noDetectionCountLimit: Int,
    private val numThreads: NumThreads,
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CardDetectorLiteViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CardDetectorLiteViewModel(
                application = application,
                modelPath = modelPath,
                cardClasses = cardClasses,
                useGpu = useGpu,
                scoreThreshold = scoreThreshold,
                cardFilters = cardFilters,
                inferenceIntervalMs = inferenceIntervalMs,
                lockOnThreshold = lockOnThreshold,
                noDetectionCountLimit = noDetectionCountLimit,
                numThreads = numThreads
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
