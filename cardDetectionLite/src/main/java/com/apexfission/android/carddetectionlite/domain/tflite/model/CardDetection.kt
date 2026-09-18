package com.apexfission.android.carddetectionlite.domain.tflite.model

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import com.apexfission.android.yolo.engine.Feature

/**
 * Encapsulates the tracking and detection result for a card in an image frame.
 *
 * @property lockOnProgress A value from 0.0 to 1.0 indicating temporal tracking progress toward locking onto a card.
 * @property id A unique identifier assigned when a card locks on (`LockingStatus.NewCard` or `LockingStatus.CardLocked`). Null while locking.
 * @property card The primary detected card [Feature].
 * @property features Secondary detected features found within the frame.
 * @property lockingStatus The current tracking lifecycle state ([LockingStatus.LockingCard], [LockingStatus.NewCard], or [LockingStatus.CardLocked]).
 * @property detectionSource The source mechanism used to track or detect the card ([DetectionSource.Yolo] or [DetectionSource.Hash]).
 */
@Immutable
@Stable
data class CardDetection(
    val lockOnProgress: Float,
    val id: Long?,
    val card: Feature,
    val features: List<Feature>,
    val lockingStatus: LockingStatus,
    val detectionSource: DetectionSource = DetectionSource.Yolo,
)

/**
 * Represents the source mechanism used to generate a card detection.
 */
@Immutable
@Stable
enum class DetectionSource {
    /** The card was detected via YOLO object detector inference. */
    Yolo,

    /** The card was tracked via difference hash (dHash) visual similarity matching. */
    Hash
}

/**
 * Represents the temporal tracking state of a detected card.
 */
@Immutable
@Stable
enum class LockingStatus {
    /** The detector is evaluating a candidate card over consecutive frames to confirm stability. */
    LockingCard,

    /** The card has achieved temporal lock-on for the first time; a new card ID has been assigned. */
    NewCard,

    /** The card remains locked on across subsequent frames under the same assigned card ID. */
    CardLocked
}
