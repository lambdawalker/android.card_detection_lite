package com.apexfission.android.carddetectionlite.domain.tflite.model

data class CardDetection2(
    val lockOnProgress: Float,
    val id: Long?,
    val card: Feature,
    val features: List<Feature>,
    val lockingStatus: LockingStatus,
)

enum class LockingStatus {
    LockingCard,
    NewCard,
    CardLocked
}