package com.apexfission.android.carddetectionlite.ui.detector

import com.apexfission.android.carddetectionlite.domain.tflite.model.CardDetection
import java.util.concurrent.atomic.AtomicLong

/** A completed detector evaluation. [sequence] changes even when [detection] remains `null`. */
internal data class DetectionFrame(
    val sequence: Long,
    val detection: CardDetection?,
) {
    companion object {
        val Initial = DetectionFrame(sequence = 0L, detection = null)
    }
}

/** Assigns a unique sequence to every completed detector evaluation. */
internal class DetectionFrameSequencer {
    private val sequence = AtomicLong(0L)

    fun next(detection: CardDetection?): DetectionFrame =
        DetectionFrame(sequence = sequence.incrementAndGet(), detection = detection)

    fun reset(): DetectionFrame {
        sequence.set(0L)
        return DetectionFrame.Initial
    }
}

/** Counts each sequenced miss once, independent of unrelated Compose recompositions. */
internal class ConsecutiveMissTracker {
    private var lastSequence = 0L
    var count: Int = 0
        private set

    fun record(sequence: Long, detected: Boolean): Int {
        if (sequence == 0L) {
            reset()
        } else if (sequence != lastSequence) {
            lastSequence = sequence
            count = if (detected) 0 else count + 1
        }
        return count
    }

    fun reset() {
        lastSequence = 0L
        count = 0
    }
}
