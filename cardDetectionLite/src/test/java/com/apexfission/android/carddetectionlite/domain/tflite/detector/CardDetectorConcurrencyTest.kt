package com.apexfission.android.carddetectionlite.domain.tflite.detector

import android.graphics.Bitmap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.max
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class CardDetectorConcurrencyTest {

    @Test
    fun `concurrent track calls are serialized through state processing`() {
        val firstInferenceStarted = CountDownLatch(1)
        val secondInferenceStarted = CountDownLatch(1)
        val releaseFirstInference = CountDownLatch(1)
        val invocationCount = AtomicInteger(0)
        val activeCalls = AtomicInteger(0)
        val maximumActiveCalls = AtomicInteger(0)
        val yoloDetector = mock<YoloDetector>()

        whenever(yoloDetector.detect(any<Bitmap>())).doAnswer {
            val invocation = invocationCount.incrementAndGet()
            val active = activeCalls.incrementAndGet()
            maximumActiveCalls.updateAndGet { current -> max(current, active) }

            try {
                if (invocation == 1) {
                    firstInferenceStarted.countDown()
                    check(releaseFirstInference.await(5, TimeUnit.SECONDS)) {
                        "Timed out waiting to release the first inference"
                    }
                } else {
                    secondInferenceStarted.countDown()
                }
            } finally {
                activeCalls.decrementAndGet()
            }

            throw InferenceProbeComplete
        }

        val detector = CardDetector(
            yoloDetector = yoloDetector,
            cardClasses = setOf(0)
        )
        val bitmap = mock<Bitmap>()
        val executor = Executors.newFixedThreadPool(2)

        try {
            val first = executor.submit { runCatching { detector.track(bitmap) } }
            check(firstInferenceStarted.await(5, TimeUnit.SECONDS)) {
                "The first inference did not start"
            }

            val second = executor.submit { runCatching { detector.track(bitmap) } }
            val overlapped = secondInferenceStarted.await(500, TimeUnit.MILLISECONDS)

            releaseFirstInference.countDown()
            first.get(5, TimeUnit.SECONDS)
            second.get(5, TimeUnit.SECONDS)

            assertFalse("A second track call entered before the first completed", overlapped)
            assertEquals(1, maximumActiveCalls.get())
        } finally {
            releaseFirstInference.countDown()
            executor.shutdownNow()
        }
    }

    private object InferenceProbeComplete : RuntimeException()
}
