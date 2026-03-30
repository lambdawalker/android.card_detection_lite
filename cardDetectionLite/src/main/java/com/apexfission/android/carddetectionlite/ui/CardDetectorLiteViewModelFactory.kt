package com.apexfission.android.carddetectionlite.ui

import android.app.Application
import androidx.compose.ui.unit.IntSize
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.apexfission.android.carddetectionlite.domain.tflite.detector.InputShape
import com.apexfission.android.carddetectionlite.domain.tflite.filters.CardValidator
import kotlinx.coroutines.flow.MutableStateFlow

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
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CardDetectorLiteViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST") return CardDetectorLiteViewModel(
                application, modelPath, cardClasses, useGpu, scoreThreshold, cardFilters, canvasSize, imageMode
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}