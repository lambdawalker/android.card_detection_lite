package com.apexfission.android.carddetectiontest


import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apexfission.android.carddetectionlite.domain.tflite.model.CardDetection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel : ViewModel() {
    private val _isDetectionEnabled = MutableStateFlow(true)
    val isDetectionEnabled = _isDetectionEnabled.asStateFlow()
    val useCloud: Boolean = false

    fun onDetection(card: CardDetection) {
        if (!card.isNewDetection && card.id == null) return
        if (!_isDetectionEnabled.value) return

        viewModelScope.launch {
            try {
                _isDetectionEnabled.value = false

                // The ViewModel doesn't care about Dispatchers;
                // it just calls the function and waits.
                if (useCloud) {
                    performCloudOcr(card)
                } else {
                    performOnDeviceOcr(card)
                }

            } catch (e: Exception) {
                Log.e("OCR", "Error processing card", e)
            } finally {
                _isDetectionEnabled.value = true
            }
        }
    }

    // OPTION A: Cloud-based (Network/IO)
    private suspend fun performCloudOcr(card: CardDetection) = withContext(Dispatchers.IO) {
        Log.d("OCR", "Running Cloud OCR (Network bound)")
        withContext(Dispatchers.IO){
            // api.uploadAndRecognize(card.image)
        }
    }

    // OPTION B: On-Device (CPU/Math)
    private suspend fun performOnDeviceOcr(card: CardDetection) = withContext(Dispatchers.Default) {
        Log.d("OCR", "Running On-Device OCR (CPU bound)")
        withContext(Dispatchers.Default){
            // localLibrary.process(card.bitmap)
        }
    }
}



