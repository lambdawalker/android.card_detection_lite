package com.apexfission.android.tflitetest


import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apexfission.android.carddetectionlite.domain.tflite.data.Detection
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {
    private val _isDetectionEnabled = MutableStateFlow(true)
    val isDetectionEnabled = _isDetectionEnabled.asStateFlow()

    private var previous: ULong = 0u
    private var count = 0

    fun onDetections(newCutouts: List<Detection>) {
        if (newCutouts.isNotEmpty() && _isDetectionEnabled.value) {
            _isDetectionEnabled.value = false

            viewModelScope.launch {
                try {
                    val cutout = newCutouts.first()

                    val current = cutout.objectBitmap.generateDHash(16)

                    val isSimilar = isVisuallySimilar(previous, current, 15)

                    if (isSimilar) {
                        count += 1
                    } else {
                        count = 0
                    }

                    if (count > 10) {
                        count = 0
                        Log.d("COMPX", "HERE")
                    }

                    previous = current
                } finally {
                    _isDetectionEnabled.value = true
                }
            }
        }
    }
}



