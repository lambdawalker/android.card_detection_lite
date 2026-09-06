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
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.apexfission.android.carddetectionlite.domain.ModelCatalog
import com.apexfission.android.carddetectionlite.domain.tflite.filters.AspectRatioValidator2
import com.apexfission.android.carddetectionlite.domain.tflite.filters.MarginValidator2
import com.apexfission.android.carddetectionlite.tfmodel.cardClasses
import com.apexfission.android.carddetectionlite.tfmodel.classes
import com.apexfission.android.carddetectionlite.tfmodel.modelPath
import com.apexfission.android.carddetectionlite.ui.simulation.CardTrackingSimulator
import com.apexfission.android.carddetectiontest.ui.theme.CardDetectionTestTheme

class CardDetectionSimulationActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val mainViewModel: MainViewModel by viewModels()

        setContent {
            CardDetectionTestTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    val isDetectionEnabled by mainViewModel.isDetectionEnabled.collectAsStateWithLifecycle()

                    val videoUri = "android.resource://$packageName/raw/sim5".toUri()

                    CardTrackingSimulator(
                        modifier = Modifier.padding(innerPadding),
                        videoUri = videoUri,
                        modelPath = ModelCatalog.TfLite.modelPath,
                        classLabels = ModelCatalog.TfLite.classes,
                        cardClasses = ModelCatalog.TfLite.cardClasses,
                        useGpu = false,
                        scoreThreshold = 0.3f,
                        showBoundingBoxes = true,
                        showClassNames = true,
                        showLockOnProgress = true,
                        isDetectionEnabled = isDetectionEnabled,
                        onCardDetection = mainViewModel::onDetection2,
                        cardFilters = listOf(
                            MarginValidator2(), AspectRatioValidator2()
                        ),
                        inferenceIntervalMs = 33L,
                        lockOnThreshold = 5,
                        noDetectionCountLimit = 8
                    )
                }
            }
        }
    }
}
