package com.apexfission.android.carddetectionlite.ui.detector

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DetectionFrameSequencerTest {

    @Test
    fun `repeated misses receive distinct sequence numbers`() {
        val sequencer = DetectionFrameSequencer()

        val firstMiss = sequencer.next(null)
        val secondMiss = sequencer.next(null)
        val thirdMiss = sequencer.next(null)

        assertNull(firstMiss.detection)
        assertNull(secondMiss.detection)
        assertNull(thirdMiss.detection)
        assertEquals(1L, firstMiss.sequence)
        assertEquals(2L, secondMiss.sequence)
        assertEquals(3L, thirdMiss.sequence)
        assertNotEquals(firstMiss, secondMiss)
        assertNotEquals(secondMiss, thirdMiss)
    }

    @Test
    fun `reset returns the non-frame sentinel and restarts sequencing`() {
        val sequencer = DetectionFrameSequencer()
        sequencer.next(null)
        sequencer.next(null)

        assertEquals(DetectionFrame.Initial, sequencer.reset())
        assertEquals(1L, sequencer.next(null).sequence)
    }

    @Test
    fun `miss tracker counts each sequence once and ignores coordinate recomposition`() {
        val tracker = ConsecutiveMissTracker()

        assertEquals(1, tracker.record(sequence = 1L, detected = false))
        assertEquals(1, tracker.record(sequence = 1L, detected = false))
        assertEquals(2, tracker.record(sequence = 2L, detected = false))
    }

    @Test
    fun `detection resets misses and initial state is not a miss`() {
        val tracker = ConsecutiveMissTracker()

        assertEquals(0, tracker.record(sequence = 0L, detected = false))
        assertEquals(1, tracker.record(sequence = 1L, detected = false))
        assertEquals(0, tracker.record(sequence = 2L, detected = true))
        assertEquals(1, tracker.record(sequence = 3L, detected = false))
    }
}
