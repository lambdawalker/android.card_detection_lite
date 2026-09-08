package com.apexfission.android.carddetectiontest

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apexfission.android.carddetectionlite.domain.tflite.model.CardDetection
import com.apexfission.android.carddetectionlite.domain.tflite.model.LockingStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel : ViewModel() {
    private val _isDetectionEnabled = MutableStateFlow(true)
    val isDetectionEnabled = _isDetectionEnabled.asStateFlow()

    private val _navigateBack = MutableStateFlow(false)
    val navigateBack = _navigateBack.asStateFlow()

    val useCloud: Boolean = false

    /**
     * Responds to back navigation requests
     */
    fun onBackRequested() {
        Log.d("UserRequest", "onBackRequested")
        _navigateBack.value = true
    }

    /**
     * Resets back navigation state after the Activity handles navigation.
     */
    fun onBackHandled() {
        _navigateBack.value = false
    }

    /**
     * Responds to explicit capture requests
     *
     * @param card The latest valid [CardDetection] stored by the detector.
     */
    fun onCaptureRequested(card: CardDetection?) {
        Log.d("UserRequest", "Processing card id: ${card?.id}, locking status: ${card?.lockingStatus}")
        if (card == null) return
        processCard(card)
    }

    /**
     * Responds to continuous frame-by-frame detections.
     */
    fun onDetection(card: CardDetection) {
        Log.d("OnDetection", "Processing card id: ${card.id}, locking status: ${card.lockingStatus}")
        if (card.lockingStatus != LockingStatus.NewCard && card.id == null) return
        if (!_isDetectionEnabled.value) return
        processCard(card)
    }

    private fun processCard(card: CardDetection) {
        viewModelScope.launch {
            try {
                _isDetectionEnabled.value = false

                if (useCloud) {
                    performCloudOcr(card)
                } else {
                    performOnDeviceOcr(card)
                }

            } catch (e: Exception) {
                Log.e("MainViewModel", "Error processing card", e)
            } finally {
                _isDetectionEnabled.value = true
            }
        }
    }

    // OPTION A: Cloud-based (Network/IO)
    private suspend fun performCloudOcr(card: CardDetection) = withContext(Dispatchers.IO) {
        Log.d("MainViewModel", "Running Cloud OCR (Network bound)")
        withContext(Dispatchers.IO) {
            // api.uploadAndRecognize(card.image)
        }
    }

    // OPTION B: On-Device (CPU/Math)
    private suspend fun performOnDeviceOcr(card: CardDetection) = withContext(Dispatchers.Default) {
        Log.d("MainViewModel", "Running On-Device OCR (CPU bound)")
        withContext(Dispatchers.Default) {
            // localLibrary.process(card.bitmap)
        }
    }
}
