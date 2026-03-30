package com.apexfission.android.carddetectionlite.ui

import android.app.Application
import androidx.compose.ui.unit.IntSize
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.apexfission.android.carddetectionlite.domain.tflite.detector.InputShape
import com.apexfission.android.carddetectionlite.domain.tflite.filters.CardValidator
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * A [ViewModelProvider.Factory] responsible for creating instances of [CardDetectorLiteViewModel].
 *
 * This factory is necessary because `CardDetectorLiteViewModel` has a non-empty constructor
 * with several dependencies. The Android ViewModel framework requires a factory to know how
 * to instantiate such ViewModels. This class takes in all the required configuration parameters
 * and passes them to the ViewModel's constructor.
 *
 * @property application The application instance, needed by `AndroidViewModel`.
 * @property modelPath The asset path for the TFLite model.
 * @property useGpu A flag to enable or disable the GPU delegate.
 * @property scoreThreshold The minimum confidence for raw detections.
 * @property cardFilters A list of custom [CardValidator]s.
 * @property canvasSize A `StateFlow` that communicates the UI's size to the ViewModel.
 * @property imageMode The chosen [InputShape] for image preprocessing.
 * @property cardClasses The list of class IDs to be treated as primary card targets.
 */
class CardDetectorLiteViewModelFactory(
    private val application: Application,
    private val modelPath: String,
    private val useGpu: Boolean,
    private val scoreThreshold: Float,
    private val cardFilters: List<CardValidator>,
    private val canvasSize: MutableStateFlow<IntSize>,
    private val imageMode: InputShape,
    private val cardClasses: List<Int>,
) : ViewModelProvider.Factory {

    /**
     * Creates a new instance of [CardDetectorLiteViewModel].
     *
     * @param modelClass The class of the ViewModel to create.
     * @return A newly instantiated [CardDetectorLiteViewModel].
     * @throws IllegalArgumentException if the requested `modelClass` is not assignable
     *         from `CardDetectorLiteViewModel`.
     */
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CardDetectorLiteViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CardDetectorLiteViewModel(
                application, modelPath, cardClasses, useGpu, scoreThreshold, cardFilters, canvasSize, imageMode
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
