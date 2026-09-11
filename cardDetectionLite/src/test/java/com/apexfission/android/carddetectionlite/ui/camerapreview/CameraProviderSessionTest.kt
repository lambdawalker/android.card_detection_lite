package com.apexfission.android.carddetectionlite.ui.camerapreview

import java.util.concurrent.atomic.AtomicInteger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CameraProviderSessionTest {
    @Test
    fun `listener skips work when disposal happens before provider completion`() {
        val session = CameraProviderSession()

        session.dispose()

        assertFalse(session.isActive())
    }

    @Test
    fun `binding that races with disposal is cleaned immediately`() {
        val session = CameraProviderSession()
        val cleanupCount = AtomicInteger(0)
        assertTrue(session.isActive())

        session.dispose()
        val retained = session.completeBinding { cleanupCount.incrementAndGet() }

        assertFalse(retained)
        assertEquals(1, cleanupCount.get())
    }

    @Test
    fun `completed binding is cleaned exactly once on disposal`() {
        val session = CameraProviderSession()
        val cleanupCount = AtomicInteger(0)
        assertTrue(session.completeBinding { cleanupCount.incrementAndGet() })

        session.dispose()
        session.dispose()

        assertEquals(1, cleanupCount.get())
    }
}
