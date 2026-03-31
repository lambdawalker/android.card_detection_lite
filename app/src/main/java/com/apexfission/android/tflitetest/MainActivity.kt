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
import com.apexfission.android.carddetectionlite.domain.ModelCatalog
import com.apexfission.android.carddetectionlite.domain.tflite.detector.InputShape
import com.apexfission.android.carddetectionlite.domain.tflite.filters.AspectRatioValidator
import com.apexfission.android.carddetectionlite.domain.tflite.filters.CenterProximityValidator
import com.apexfission.android.carddetectionlite.domain.tflite.filters.MarginValidator
import com.apexfission.android.carddetectionlite.tfmodel.cardClasses
import com.apexfission.android.carddetectionlite.tfmodel.classes
import com.apexfission.android.carddetectionlite.tfmodel.modelPath
import com.apexfission.android.carddetectionlite.ui.CardDetectorLite
import com.apexfission.android.carddetectionlite.ui.permissions.HandleCameraPermission
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
                            .fillMaxSize(), onBack = { finish() }, onNotNow = { finish() }) {
                        val isDetectionEnabled by mainViewModel.isDetectionEnabled.collectAsStateWithLifecycle()

                        CardDetectorLite(
                            modifier = Modifier.padding(innerPadding),
                            modelPath = ModelCatalog.TfLite.modelPath,
                            classLabels = ModelCatalog.TfLite.classes,
                            cardClasses = ModelCatalog.TfLite.cardClasses,
                            useGpu = true,
                            scoreThreshold = 0.6f,
                            showBoundingBoxes = false,
                            showClassNames = false,
                            showFlashlightSwitch = true,
                            isDetectionEnabled = isDetectionEnabled,
                            onCardDetection = {
                                mainViewModel.onDetections(it)
                            },
                            cardFilters = listOf(
                                MarginValidator(), AspectRatioValidator(), CenterProximityValidator()
                            ),
                            imageMode = InputShape.SquareCrop,
                            inferenceIntervalMs = 33L,
                            tapToFocusEnabled = true,
                            focusOnCardEnabled = true,
                            lockOnThreshold = 4
                        )
                    }
                }
            }
        }
    }
}
