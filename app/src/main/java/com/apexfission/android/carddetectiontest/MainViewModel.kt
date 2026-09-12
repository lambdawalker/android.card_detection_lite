package com.apexfission.android.carddetectiontest

import android.graphics.Bitmap
import android.util.Log
import androidx.annotation.WorkerThread
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apexfission.android.carddetectionlite.domain.tflite.model.CardDetection
import com.apexfission.android.carddetectionlite.domain.tflite.model.LockingStatus
import com.apexfission.android.carddetectionlite.resource.use
import com.apexfission.android.carddetectionlite.ui.detector.CardCaptureCallback
import com.apexfission.android.carddetectionlite.ui.detector.CardDetectionCallback
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel : ViewModel(), CardDetectionCallback, CardCaptureCallback {
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
    @WorkerThread
    override fun onCapture(card: CardDetection, bitmap: Bitmap) {
        Log.d("UserRequest", "Processing card id: ${card.id}, locking status: ${card.lockingStatus}")
        processCard(card, bitmap)
    }

    /**
     * Responds to continuous frame-by-frame detections.
     */
    @WorkerThread
    override fun onCardDetection(card: CardDetection, bitmap: Bitmap) {
        Log.d("OnDetection", "Processing card id: ${card.id}, locking status: ${card.lockingStatus}")
        if (card.lockingStatus != LockingStatus.NewCard && card.id == null) {
            bitmap.recycle()
            return
        }
        if (!_isDetectionEnabled.value) {
            bitmap.recycle()
            return
        }
        processCard(card, bitmap)
    }

    private fun processCard(card: CardDetection, bitmap: Bitmap) {
        viewModelScope.launch {
            bitmap.use {
                try {
                    _isDetectionEnabled.value = false

                    if (useCloud) {
                        performCloudOcr(card, it)
                    } else {
                        performOnDeviceOcr(card, it)
                    }

                } catch (e: Exception) {
                    Log.e("MainViewModel", "Error processing card", e)
                } finally {
                    _isDetectionEnabled.value = true
                }
            }
        }
    }

    // OPTION A: Cloud-based (Network/IO)
    private suspend fun performCloudOcr(card: CardDetection, bitmap: Bitmap) = withContext(Dispatchers.IO) {
        Log.d("MainViewModel", "Running Cloud OCR (Network bound)")
        // api.uploadAndRecognize(bitmap)
    }

    // OPTION B: On-Device (CPU/Math)
    private suspend fun performOnDeviceOcr(card: CardDetection, bitmap: Bitmap) = withContext(Dispatchers.Default) {
        Log.d("MainViewModel", "Running On-Device OCR (CPU bound)")
        // localLibrary.process(bitmap)
    }
}
