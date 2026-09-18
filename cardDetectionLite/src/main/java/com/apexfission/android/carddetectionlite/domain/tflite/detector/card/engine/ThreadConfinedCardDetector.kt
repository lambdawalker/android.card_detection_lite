package com.apexfission.android.carddetectionlite.domain.tflite.detector.card.engine

import android.graphics.Bitmap
import androidx.camera.core.ImageProxy
import com.apexfission.android.yolo.tflite.engine.EngineThreadDispatcher
import com.apexfission.android.yolo.tflite.engine.ThreadConfinedResource
import com.apexfission.android.carddetectionlite.domain.tflite.model.CardDetection

/** Confines native construction, operations and cleanup to an owned or shared physical thread. */
class ThreadConfinedCardDetector private constructor(
    private val resource: ThreadConfinedResource<CardDetector>,
) : CardDetector by resource.value {
    constructor(
        sharedDispatcher: EngineThreadDispatcher? = null,
        detectorFactory: () -> CardDetector,
    ) : this(ThreadConfinedResource(sharedDispatcher, detectorFactory))

    internal val detectorThreadId: Long = resource.call({ -1L }) { Thread.currentThread().id }
    internal var lastTrackThreadId: Long = -1L
        private set
    internal var closeThreadId: Long = -1L
        private set

    override fun track(bitmap: Bitmap): CardDetection? = resource.call({ null }) {
        lastTrackThreadId = Thread.currentThread().id
        it.track(bitmap)
    }

    override fun track(imageProxy: ImageProxy): CardDetection? = resource.call({ null }) {
        lastTrackThreadId = Thread.currentThread().id
        it.track(imageProxy)
    }

    override var enabled: Boolean
        get() = resource.call({ false }) { it.enabled }
        set(value) {
            resource.call({ }) { it.enabled = value }
        }

    override fun close() {
        resource.close()
        closeThreadId = resource.closeThreadId
    }
}
