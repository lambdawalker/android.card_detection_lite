package com.apexfission.android.tflitetest

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apexfission.android.carddetectionlite.domain.ocr.OcrResult
import com.apexfission.android.carddetectionlite.domain.ocr.OcrWrapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel : ViewModel() {
    private val _isDetectionEnabled = MutableStateFlow(true)
    val isDetectionEnabled = _isDetectionEnabled.asStateFlow()

    private val _ocrResult = MutableStateFlow<List<OcrResult>>(emptyList())
    private val ocrWrapper = OcrWrapper()

    fun onDetections(newCutouts: List<Bitmap>) {
//
//        if (newCutouts.isNotEmpty() && _isDetectionEnabled.value) {
//            _isDetectionEnabled.value = false
//            viewModelScope.launch {
//                try {
//                    val cutout = newCutouts.first()
//
//                    val result = withContext(Dispatchers.IO) {
//                        ocrWrapper.run(cutout)
//                    }
//                    _ocrResult.value = result
//
//                    result.forEach {
//                        Log.d("OCR", "${it.text.replace("\n", " ")}, ${it.boundingBox}")
//                    }
//
//                } finally {
//                    _isDetectionEnabled.value = true
//                }
//            }
//        }
    }
}