package com.apexfission.android.carddetectionlite.domain.tflite.filters

import com.apexfission.android.carddetectionlite.domain.tflite.data.RawDet
import org.junit.Assert.assertEquals
import org.junit.Test

class DetectionFilterTest {

    private val imageWidth = 1000
    private val imageHeight = 500

    // --- MarginFilter Tests ---

    @Test
    fun `MarginFilter keeps detections within margin`() {
        val filter = MarginFilter(margin = 20)
        val detections = listOf(
            RawDet(0.02f, 0.04f, 0.98f, 0.96f, 1f, 0) // x1=20, y1=20, x2=980, y2=480
        )
        val filtered = filter.filter(detections, imageWidth, imageHeight)
        assertEquals(1, filtered.size)
    }

    @Test
    fun `MarginFilter removes detections outside margin`() {
        val filter = MarginFilter(margin = 20)
        val detections = listOf(
            RawDet(0.01f, 0.04f, 0.5f, 0.5f, 1f, 0), // Too close to left
            RawDet(0.1f, 0.03f, 0.5f, 0.5f, 1f, 0), // Too close to top
            RawDet(0.1f, 0.1f, 0.99f, 0.5f, 1f, 0), // Too close to right
            RawDet(0.1f, 0.1f, 0.5f, 0.97f, 1f, 0)  // Too close to bottom
        )
        val filtered = filter.filter(detections, imageWidth, imageHeight)
        assertEquals(0, filtered.size)
    }

    @Test
    fun `MarginFilter with zero margin keeps all detections`() {
        val filter = MarginFilter(margin = 0)
        val detections = listOf(
            RawDet(0f, 0f, 1f, 1f, 1f, 0)
        )
        val filtered = filter.filter(detections, imageWidth, imageHeight)
        assertEquals(1, filtered.size)
    }

    // --- AspectRatioFilter Tests ---

    @Test
    fun `AspectRatioFilter keeps detections with valid aspect ratio`() {
        val filter = AspectRatioFilter(minAspectRatio = 1.4f, maxAspectRatio = 1.8f)
        val detections = listOf(
            RawDet(0.1f, 0.1f, 0.258f, 0.2f, 1f, 0) // width=0.158, height=0.1, ratio=1.58
        )
        val filtered = filter.filter(detections, imageWidth, imageHeight)
        assertEquals(1, filtered.size)
    }

    @Test
    fun `AspectRatioFilter removes detections with invalid aspect ratio`() {
        val filter = AspectRatioFilter(minAspectRatio = 1.4f, maxAspectRatio = 1.8f)
        val detections = listOf(
            RawDet(0.1f, 0.1f, 0.2f, 0.2f, 1f, 0),   // Too square (ratio=1.0)
            RawDet(0.1f, 0.1f, 0.3f, 0.2f, 1f, 0)    // Too wide (ratio=2.0)
        )
        val filtered = filter.filter(detections, imageWidth, imageHeight)
        assertEquals(0, filtered.size)
    }

    @Test
    fun `AspectRatioFilter removes detections with zero area`() {
        val filter = AspectRatioFilter()
        val detections = listOf(
            RawDet(0.1f, 0.1f, 0.1f, 0.2f, 1f, 0) // Zero width
        )
        val filtered = filter.filter(detections, imageWidth, imageHeight)
        assertEquals(0, filtered.size)
    }
}