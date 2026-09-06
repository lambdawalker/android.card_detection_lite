package com.apexfission.android.carddetectionlite.ui.simulation

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.apexfission.android.carddetectionlite.domain.tflite.filters.CardValidator2
import com.apexfission.android.carddetectionlite.ui.NumThreads

class CardTrackingSimulatorViewModelFactory(
    private val application: Application,
    private val modelPath: String,
    private val cardClasses: Set<Int>,
    private val useGpu: Boolean,
    private val scoreThreshold: Float,
    private val cardFilters: List<CardValidator2>,
    private val inferenceIntervalMs: Long,
    private val lockOnThreshold: Int,
    private val noDetectionCountLimit: Int,
    private val numThreads: NumThreads,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CardTrackingSimulatorViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CardTrackingSimulatorViewModel(
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
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
