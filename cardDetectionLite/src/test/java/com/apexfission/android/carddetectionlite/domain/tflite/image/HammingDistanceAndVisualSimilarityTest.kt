package com.apexfission.android.carddetectionlite.domain.tflite.image

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HammingDistanceAndVisualSimilarityTest {

    @Test
    fun testHammingDistanceIdenticalHashes() {
        val hash = 0b10101010_11001100_11110000_00001111uL
        assertEquals(0, hash.hammingDistanceTo(hash))
    }

    @Test
    fun testHammingDistanceInverseHashes() {
        val hashA = 0x0000000000000000uL
        val hashB = 0xFFFFFFFFFFFFFFFFuL
        assertEquals(64, hashA.hammingDistanceTo(hashB))
    }

    @Test
    fun testHammingDistanceSingleBitDifference() {
        val hashA = 0b0001uL
        val hashB = 0b0000uL
        assertEquals(1, hashA.hammingDistanceTo(hashB))
    }

    @Test
    fun testIsVisuallySimilarWithHashesWithinThreshold() {
        val hashA = 0b11111111_00000000uL
        val hashB = 0b11111111_00000011uL
        assertTrue(isVisuallySimilar(hashA, hashB, differenceHashDistanceLimit = 5))
    }

    @Test
    fun testIsVisuallySimilarWithHashesExceedingThreshold() {
        val hashA = 0b11111111_00000000uL
        val hashB = 0b11111111_00111111uL
        assertFalse(isVisuallySimilar(hashA, hashB, differenceHashDistanceLimit = 5))
    }
}
