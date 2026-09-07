package com.apexfission.android.carddetectionlite.ui

import com.apexfission.android.carddetectionlite.domain.tflite.detector.PreProcessingImageTransformation
import com.apexfission.android.carddetectionlite.ui.detector.CardDetectorPreset
import com.apexfission.android.carddetectionlite.ui.detector.NumThreads
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CardDetectorPresetTest {

    @Test
    fun testHighAccuracyPresetValues() {
        val preset = CardDetectorPreset.HighAccuracy

        assertEquals(0.80f, preset.scoreThreshold, 0.001f)
        assertEquals(0.45f, preset.iouThreshold, 0.001f)
        assertEquals(6, preset.lockOnThreshold)
        assertEquals(10, preset.noDetectionCountLimit)
        assertEquals(33L, preset.inferenceIntervalMs)
        assertTrue(preset.useGpu)
        assertEquals(PreProcessingImageTransformation.FullImage, preset.preProcessingImageTransformation)
        assertEquals(NumThreads.Default, preset.numThreads)
    }

    @Test
    fun testHighPerformancePresetValues() {
        val preset = CardDetectorPreset.HighPerformance

        assertEquals(0.50f, preset.scoreThreshold, 0.001f)
        assertEquals(0.45f, preset.iouThreshold, 0.001f)
        assertEquals(4, preset.lockOnThreshold)
        assertEquals(8, preset.noDetectionCountLimit)
        assertEquals(33L, preset.inferenceIntervalMs)
        assertTrue(preset.useGpu)
        assertEquals(PreProcessingImageTransformation.SquareCrop(), preset.preProcessingImageTransformation)
        assertEquals(NumThreads.Default, preset.numThreads)
    }

    @Test
    fun testBatterySaverPresetValues() {
        val preset = CardDetectorPreset.BatterySaver

        assertEquals(0.50f, preset.scoreThreshold, 0.001f)
        assertEquals(0.45f, preset.iouThreshold, 0.001f)
        assertEquals(4, preset.lockOnThreshold)
        assertEquals(6, preset.noDetectionCountLimit)
        assertEquals(100L, preset.inferenceIntervalMs)
        assertFalse(preset.useGpu)
        assertEquals(PreProcessingImageTransformation.SquareCrop(), preset.preProcessingImageTransformation)
        assertEquals(NumThreads.CustomCount(2), preset.numThreads)
    }

    @Test
    fun testPresetCopyOverride() {
        val original = CardDetectorPreset.HighPerformance
        val modified = original.copy(scoreThreshold = 0.40f)

        assertEquals(0.40f, modified.scoreThreshold, 0.001f)
        assertEquals(original.preProcessingImageTransformation, modified.preProcessingImageTransformation)
        assertEquals(original.useGpu, modified.useGpu)
    }

    @Test
    fun testPresetChange() {
        val original = CardDetectorPreset.HighPerformance
        val modified = original.change(scoreThreshold = 0.60f, lockOnThreshold = 5)

        assertEquals(0.60f, modified.scoreThreshold, 0.001f)
        assertEquals(5, modified.lockOnThreshold)
        assertEquals(original.preProcessingImageTransformation, modified.preProcessingImageTransformation)
        assertEquals(original.useGpu, modified.useGpu)
    }
}
