package com.apexfission.android.carddetectionlite.resource

import android.graphics.Bitmap
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify

class BitmapUseTest {
    @Test
    fun bitmapUseRecyclesAfterSuccessfulBlock() {
        val bitmap = mock(Bitmap::class.java)

        bitmap.use { assertSame(bitmap, it) }

        verify(bitmap).recycle()
    }

    @Test
    fun bitmapUseRecyclesAfterFailedBlock() {
        val bitmap = mock(Bitmap::class.java)

        assertThrows(IllegalArgumentException::class.java) {
            bitmap.use { throw IllegalArgumentException("OCR failed") }
        }

        verify(bitmap).recycle()
    }
}
