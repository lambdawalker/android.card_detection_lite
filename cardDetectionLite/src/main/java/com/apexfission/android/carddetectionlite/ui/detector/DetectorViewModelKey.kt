package com.apexfission.android.carddetectionlite.ui.detector

import com.apexfission.android.carddetectionlite.domain.tflite.detector.PreProcessingImageTransformation
import com.apexfission.android.carddetectionlite.domain.tflite.filters.CardValidator
import java.security.MessageDigest

internal enum class DetectorComponent {
    CAMERA,
    SIMULATOR,
}

internal fun detectorViewModelKey(
    component: DetectorComponent,
    instanceKey: String,
    modelPath: String,
    classLabels: Map<Int, String>,
    cardClasses: Set<Int>,
    detectorPreset: CardDetectorPreset,
    cardFilters: List<CardValidator>,
): String {
    require(instanceKey.isNotBlank()) { "Detector instanceKey must not be blank." }

    val canonicalConfiguration = buildString {
        appendField(component.name)
        appendField(instanceKey)
        appendField(modelPath)
        classLabels.toSortedMap().forEach { (classId, label) ->
            appendField(classId.toString())
            appendField(label)
        }
        appendField("labels-end")
        cardClasses.sorted().forEach { appendField(it.toString()) }
        appendField("classes-end")
        appendPreset(detectorPreset)
        cardFilters.forEach { appendField(it.configurationKey) }
    }
    val digest = MessageDigest.getInstance("SHA-256")
        .digest(canonicalConfiguration.toByteArray(Charsets.UTF_8))
        .joinToString(separator = "") { byte -> "%02x".format(byte) }

    return "card-detection-lite:${component.name.lowercase()}:$instanceKey:$digest"
}

private fun StringBuilder.appendPreset(preset: CardDetectorPreset) {
    appendField(preset.scoreThreshold.toRawBits().toString())
    appendField(preset.iouThreshold.toRawBits().toString())
    appendField(preset.lockOnThreshold.toString())
    appendField(preset.noDetectionCountLimit.toString())
    appendField(preset.memoryDetectionTimeLimit.toString())
    appendField(preset.validateClassIdInLockOnProcess.toString())
    appendField(preset.differenceHashDistanceLimit.toString())
    appendField(preset.allowTemporalDrift.toString())
    appendField(preset.inferenceIntervalMs.toString())
    appendField(preset.useGpu.toString())
    appendField(preset.preProcessingImageTransformation.configurationKey())
    appendField(preset.numThreads.configurationKey())
}

private fun PreProcessingImageTransformation.configurationKey(): String = when (this) {
    PreProcessingImageTransformation.FullImage -> "full-image"
    PreProcessingImageTransformation.CenterSquareCrop -> "center-square-crop"
    is PreProcessingImageTransformation.SquareCrop -> "square-crop:${top.value.toRawBits()}"
    PreProcessingImageTransformation.CenterVisibleImage -> "center-visible-image"
    is PreProcessingImageTransformation.VisibleImage -> "visible-image:${top.value.toRawBits()}"
    PreProcessingImageTransformation.CenterVisibleImageSquareCrop -> "center-visible-square-crop"
    is PreProcessingImageTransformation.VisibleImageSquareCrop ->
        "visible-square-crop:${top.value.toRawBits()}"
}

private fun NumThreads.configurationKey(): String = when (this) {
    NumThreads.Default -> "default"
    NumThreads.Quarter -> "quarter"
    NumThreads.Half -> "half"
    NumThreads.ThreeQuarters -> "three-quarters"
    is NumThreads.CustomPercentage -> "percentage:${percentage.toRawBits()}"
    is NumThreads.CustomCount -> "count:$count"
}

private fun StringBuilder.appendField(value: String) {
    append(value.length).append(':').append(value)
}
