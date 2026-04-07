package com.apexfission.android.carddetectiontest

import android.os.Bundle
import android.util.Size
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
import com.apexfission.android.carddetectiontest.ui.theme.CardDetectionTestTheme
import com.apexfission.android.permissionscompose.HandleCameraPermission

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val mainViewModel: MainViewModel by viewModels()

        setContent {
            CardDetectionTestTheme {
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
                            showLockOnProgress=true,
                            showFocusIndicator=true,
                            showFlashlightSwitch = true,
                            analysisTargetResolution=Size(2048, 1080),
                            isDetectionEnabled = isDetectionEnabled,
                            onCardDetection = mainViewModel::onDetection,
                            cardFilters = listOf(
                                MarginValidator(), AspectRatioValidator(), CenterProximityValidator()
                            ),
                            imageMode = InputShape.SquareCrop,
                            inferenceIntervalMs = 33L,
                            tapToFocusEnabled = true,
                            focusOnCardEnabled = true,
                            lockOnThreshold = 4,
                            showDebugOverlay = true
                        )
                    }
                }
            }
        }
    }
}
