package com.apexfission.android.tflitetest

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.apexfission.android.carddetectionlite.ui.HandleCameraPermission
import com.apexfission.android.carddetectionlite.ui.CardDetectorLite
import com.apexfission.android.tflitetest.ui.theme.TfLiteTestTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val mainViewModel: MainViewModel by viewModels()

        setContent {
            TfLiteTestTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->

                    HandleCameraPermission(
                        modifier = Modifier
                            .padding(innerPadding)
                            .fillMaxSize()
                    ) {
                        val isDetectionEnabled by mainViewModel.isDetectionEnabled.collectAsStateWithLifecycle()

                        CardDetectorLite(
                            modelName = "E83Y11_640B1F16.tflite",
                            useGpu = true,
                            showBoundingBoxes = true,
                            showClassNames = true,
                            showFlashlightSwitch = true,
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
