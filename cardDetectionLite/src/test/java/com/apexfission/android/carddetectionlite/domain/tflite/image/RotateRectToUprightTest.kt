package com.apexfission.android.carddetectionlite.domain.tflite.image

import android.graphics.Rect
import org.junit.Assert.assertEquals
import org.junit.Test

class RotateRectToUprightTest {

    private val rect = Rect(10, 20, 30, 40)
    private val srcW = 100
    private val srcH = 200

    @Test
    fun testRotationZeroReturnsSameRect() {
        val result = rotateRectToUpright(rect, srcW, srcH, 0)
        assertEquals(rect, result)
    }

    @Test
    fun testRotation90() {
        val expected = Rect(20, 70, 40, 90)
        val result = rotateRectToUpright(rect, srcW, srcH, 90)
        assertEquals(expected, result)
    }

    @Test
    fun testRotation180() {
        val expected = Rect(70, 160, 90, 180)
        val result = rotateRectToUpright(rect, srcW, srcH, 180)
        assertEquals(expected, result)
    }

    @Test
    fun testRotation270() {
        val expected = Rect(160, 10, 180, 30)
        val result = rotateRectToUpright(rect, srcW, srcH, 270)
        assertEquals(expected, result)
    }

    @Test
    fun testNegativeRotationNormalizesCorrectly() {
        val result = rotateRectToUpright(rect, srcW, srcH, -90)
        val expected = rotateRectToUpright(rect, srcW, srcH, 270)
        assertEquals(expected, result)
    }
}
