package com.apexfission.android.carddetectionlite.domain.tflite.detector.card.tracking

import android.graphics.Bitmap
import com.apexfission.android.carddetectionlite.domain.coordinates.models.ImageBox
import com.apexfission.android.carddetectionlite.domain.tflite.model.Detection
import com.apexfission.android.carddetectionlite.domain.tflite.model.LockingStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class CardLockStateMachineTest {

    @Test
    fun `progresses locking status after threshold matching frames`() {
        val stateMachine = CardLockStateMachine(
            lockOnThreshold = 3,
            memoryDetectionTimeLimit = 1000L
        )

        val bitmap = mock<Bitmap>()
        whenever(bitmap.width).thenReturn(200)
        whenever(bitmap.height).thenReturn(200)
        val detection = Detection(ImageBox.from2P(0, 0, 100, 100), 0.9f, 0)

        // Frame 1: LockingCard (progress 0.33)
        val f1 = stateMachine.processDetection(detection, bitmap, 1000L)
        assertEquals(LockingStatus.LockingCard, f1.lockingStatus)
        assertNull(f1.id)
        assertEquals(0.333f, f1.lockOnProgress, 0.01f)

        // Frame 2: LockingCard (progress 0.66)
        val f2 = stateMachine.processDetection(detection, bitmap, 1100L)
        assertEquals(LockingStatus.LockingCard, f2.lockingStatus)
        assertNull(f2.id)

        // Frame 3: NewCard (progress 1.0, ID assigned = 1)
        val f3 = stateMachine.processDetection(detection, bitmap, 1200L)
        assertEquals(LockingStatus.NewCard, f3.lockingStatus)
        assertEquals(1L, f3.id)
        assertEquals(1.0f, f3.lockOnProgress, 0.01f)

        // Frame 4: CardLocked (progress 1.0, ID remains 1)
        val f4 = stateMachine.processDetection(detection, bitmap, 1300L)
        assertEquals(LockingStatus.CardLocked, f4.lockingStatus)
        assertEquals(1L, f4.id)
    }

    @Test
    fun `resets tracking state after missing detection limit reached`() {
        val stateMachine = CardLockStateMachine(
            lockOnThreshold = 2,
            noDetectionCountLimit = 2
        )

        val bitmap = mock<Bitmap>()
        whenever(bitmap.width).thenReturn(200)
        whenever(bitmap.height).thenReturn(200)
        val detection = Detection(ImageBox.from2P(0, 0, 100, 100), 0.9f, 0)

        stateMachine.processDetection(detection, bitmap, 1000L)
        assertEquals(detection, stateMachine.previousCardDetection)

        stateMachine.handleMissingDetection() // count = 1
        assertEquals(detection, stateMachine.previousCardDetection)

        stateMachine.handleMissingDetection() // count = 2 (limit reached -> reset)
        assertNull(stateMachine.previousCardDetection)
    }
}
