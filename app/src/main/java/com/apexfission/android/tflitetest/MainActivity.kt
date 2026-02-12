package com.apexfission.android.tflitetest

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.apexfission.android.carddetectionlite.ui.YoloCameraScreen
import com.apexfission.android.tflitetest.ui.theme.TfLiteTestTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 7)
        }

        val mainViewModel: MainViewModel by viewModels()

        setContent {
            TfLiteTestTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Surface(
                        modifier = Modifier
                            .padding(innerPadding)
                            .fillMaxSize()
                    ) {
                        val isDetectionEnabled by mainViewModel.isDetectionEnabled.collectAsStateWithLifecycle()
                        YoloCameraScreen(
                            modelName = "E38Y11_640B1F16.tflite",
                            isDetectionEnabled = isDetectionEnabled,
                            onDetections = {
                                mainViewModel.onDetections(it)
                            }
                        )
                    }
                }
            }
        }
    }
}
