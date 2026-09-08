package com.apexfission.android.carddetectiontest

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
import com.apexfission.android.carddetectionlite.tfmodel.cardClasses
import com.apexfission.android.carddetectionlite.tfmodel.classes
import com.apexfission.android.carddetectionlite.tfmodel.modelPath
import com.apexfission.android.carddetectionlite.ui.camerapreview.CameraPreset
import com.apexfission.android.carddetectionlite.ui.detector.CardDetectorLite
import com.apexfission.android.carddetectionlite.ui.detector.CardDetectorPreset
import com.apexfission.android.carddetectionlite.ui.overlays.CardLockOnOverlay
import com.apexfission.android.carddetectionlite.ui.overlays.CardLockOnOverlay2
import com.apexfission.android.carddetectionlite.ui.overlays.DebugOverlay
import com.apexfission.android.carddetectionlite.ui.overlays.DetectionOverlay
import com.apexfission.android.carddetectionlite.ui.overlays.IdCaptureOverlay
import com.apexfission.android.carddetectiontest.ui.theme.CardDetectionTestTheme
import com.apexfission.android.permissionscompose.HandleCameraPermission

class CardDetectionActivity : ComponentActivity() {
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
                            detectorPreset = CardDetectorPreset.HighPerformance.copy(scoreThreshold = 0.8f),
                            cameraPreset = CameraPreset.Default,
                            isDetectionEnabled = isDetectionEnabled,
                            onCardDetection = mainViewModel::onDetection,
                            onBack = { finish() },
                            controlOverlay = {
//                                IdCaptureOverlay()
                                //CardLockOnOverlay()
                                CardLockOnOverlay2()
//                                DebugOverlay()
//                                DetectionOverlay()
                            }
                        )
                    }
                }
            }
        }
    }
}
