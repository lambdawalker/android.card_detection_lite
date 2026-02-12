package com.apexfission.android.tflitetest

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {

    private val _cutouts = MutableStateFlow<List<Bitmap>>(emptyList())
    val cutouts = _cutouts.asStateFlow()

    private val _isDetectionEnabled = MutableStateFlow(true)
    val isDetectionEnabled = _isDetectionEnabled.asStateFlow()

    fun onDetections(newCutouts: List<Bitmap>) {
        viewModelScope.launch {
            _cutouts.value = newCutouts
        }
    }

    fun setDetectionEnabled(enabled: Boolean) {
        _isDetectionEnabled.value = enabled
    }
}
