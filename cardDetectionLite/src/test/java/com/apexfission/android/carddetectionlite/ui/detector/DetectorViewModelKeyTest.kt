package com.apexfission.android.carddetectionlite.ui.detector

import androidx.compose.ui.unit.dp
import com.apexfission.android.carddetectionlite.domain.tflite.detector.PreProcessingImageTransformation
import com.apexfission.android.carddetectionlite.domain.tflite.filters.AspectRatioValidator
import com.apexfission.android.carddetectionlite.domain.tflite.filters.MarginValidator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class DetectorViewModelKeyTest {

    @Test
    fun siblingInstanceKeysProduceDifferentViewModelKeys() {
        assertNotEquals(key(instanceKey = "front"), key(instanceKey = "back"))
    }

    @Test
    fun detectorAndSimulatorNeverShareAViewModelKey() {
        assertNotEquals(
            key(component = DetectorComponent.CAMERA),
            key(component = DetectorComponent.SIMULATOR),
        )
    }

    @Test
    fun equivalentImmutableConfigurationsProduceTheSameViewModelKey() {
        val first = key(
            classLabels = linkedMapOf(1 to "back", 0 to "front"),
            cardClasses = linkedSetOf(1, 0),
            cardFilters = listOf(MarginValidator(), AspectRatioValidator()),
        )
        val second = key(
            classLabels = linkedMapOf(0 to "front", 1 to "back"),
            cardClasses = linkedSetOf(0, 1),
            cardFilters = listOf(MarginValidator(), AspectRatioValidator()),
        )

        assertEquals(first, second)
    }

    @Test
    fun detectorConfigurationChangesProduceDifferentViewModelKeys() {
        val baseline = key()
        val baselinePreset = preset()
        val changedKeys = buildList {
            add(key(modelPath = "models/other.tflite"))
            add(key(classLabels = mapOf(0 to "other")))
            add(key(cardClasses = setOf(1)))
            add(key(cardFilters = listOf(MarginValidator(40u))))
            listOf(
                baselinePreset.copy(scoreThreshold = 0.8f),
                baselinePreset.copy(iouThreshold = 0.2f),
                baselinePreset.copy(lockOnThreshold = 20),
                baselinePreset.copy(noDetectionCountLimit = 20),
                baselinePreset.copy(memoryDetectionTimeLimit = 5_000L),
                baselinePreset.copy(validateClassIdInLockOnProcess = false),
                baselinePreset.copy(differenceHashDistanceLimit = 5),
                baselinePreset.copy(allowTemporalDrift = false),
                baselinePreset.copy(inferenceIntervalMs = 100L),
                baselinePreset.copy(useGpu = false),
                baselinePreset.copy(
                    preProcessingImageTransformation = PreProcessingImageTransformation.SquareCrop(2.dp)
                ),
                baselinePreset.copy(numThreads = NumThreads.CustomCount(2)),
            ).forEach { changedPreset -> add(key(detectorPreset = changedPreset)) }
        }

        changedKeys.forEach { changed -> assertNotEquals(baseline, changed) }
    }

    @Test
    fun blankInstanceKeysAreRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            key(instanceKey = "  ")
        }
    }

    private fun key(
        component: DetectorComponent = DetectorComponent.CAMERA,
        instanceKey: String = "primary",
        modelPath: String = "models/card.tflite",
        classLabels: Map<Int, String> = mapOf(0 to "front"),
        cardClasses: Set<Int> = setOf(0),
        detectorPreset: CardDetectorPreset = preset(),
        cardFilters: List<com.apexfission.android.carddetectionlite.domain.tflite.filters.CardValidator> =
            listOf(MarginValidator(), AspectRatioValidator()),
    ): String = detectorViewModelKey(
        component = component,
        instanceKey = instanceKey,
        modelPath = modelPath,
        classLabels = classLabels,
        cardClasses = cardClasses,
        detectorPreset = detectorPreset,
        cardFilters = cardFilters,
    )

    private fun preset(): CardDetectorPreset = CardDetectorPreset.HighPerformance
}
