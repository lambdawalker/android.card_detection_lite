package com.apexfission.android.carddetectionlite.domain.tflite.detector.card.engine

import android.graphics.Bitmap
import androidx.camera.core.ImageProxy
import com.apexfission.android.yolo.engine.YoloDetector
import com.apexfission.android.carddetectionlite.domain.tflite.model.CardDetection
import java.io.Closeable

/**
 * Defines the contract for a higher-level card detector built on top of [YoloDetector].
 */
interface CardDetector : Closeable {

    /**
     * Controls the enabled state of the underlying detector.
     */
    var enabled: Boolean

    /**
     * Tracks a card from an [ImageProxy].
     */
    fun track(imageProxy: ImageProxy): CardDetection?

    /**
     * Tracks a card from an upright [Bitmap].
     */
    fun track(bitmap: Bitmap): CardDetection?
}
