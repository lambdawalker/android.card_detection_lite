package com.apexfission.android.carddetectionlite.domain.ocr

import android.graphics.Bitmap
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test
import org.mockito.kotlin.mock

class OcrWrapperLifecycleTest {
    @Test
    fun `close is idempotent and processing after close is rejected`() = runBlocking {
        val recognizer = FakeOcrRecognizer()
        val wrapper = OcrWrapper(recognizer)

        wrapper.close()
        wrapper.close()

        assertEquals(1, recognizer.closeCount.get())
        val error = try {
            wrapper.run(mock<Bitmap>())
            fail("Expected processing after close to fail")
            error("unreachable")
        } catch (expected: IllegalStateException) {
            expected
        }
        assertEquals("OcrWrapper is closed", error.message)
    }

    @Test
    fun `cancelling caller cancels suspended recognition`() = runBlocking {
        val recognizer = FakeOcrRecognizer(suspendForever = true)
        val wrapper = OcrWrapper(recognizer)
        val job = launch { wrapper.run(mock<Bitmap>()) }
        recognizer.started.await()

        job.cancelAndJoin()

        assertEquals(true, job.isCancelled)
        assertEquals(1, recognizer.cancellationCount.get())
        wrapper.close()
    }

    @Test
    fun `close waits for active recognition before closing recognizer`() = runBlocking {
        val recognizer = FakeOcrRecognizer(suspendForever = true)
        val wrapper = OcrWrapper(recognizer)
        val job = launch { wrapper.run(mock<Bitmap>()) }
        recognizer.started.await()

        wrapper.close()

        assertEquals(0, recognizer.closeCount.get())
        job.cancelAndJoin()
        assertEquals(1, recognizer.closeCount.get())
    }

    private class FakeOcrRecognizer(
        private val suspendForever: Boolean = false,
    ) : OcrRecognizer {
        val closeCount = AtomicInteger(0)
        val cancellationCount = AtomicInteger(0)
        val started = CompletableDeferred<Unit>()

        override suspend fun process(image: Bitmap): List<OcrResult> {
            started.complete(Unit)
            if (!suspendForever) return emptyList()
            try {
                awaitCancellation()
            } catch (error: CancellationException) {
                cancellationCount.incrementAndGet()
                throw error
            }
        }

        override fun close() {
            closeCount.incrementAndGet()
        }
    }
}
