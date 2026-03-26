package com.apexfission.android.tflitetest



import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apexfission.android.carddetectionlite.domain.tflite.model.CardDetection
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {
    private val _isDetectionEnabled = MutableStateFlow(true)
    val isDetectionEnabled = _isDetectionEnabled.asStateFlow()

    fun onDetections(card: CardDetection) {
        _isDetectionEnabled.value = false

        viewModelScope.launch {
            try {

            } finally {
                _isDetectionEnabled.value = true
            }
        }

    }
}



