package com.apexfission.android.carddetectionlite.ui.detector

import java.util.concurrent.atomic.AtomicBoolean

/**
 * Allows only one frame-processing operation to run at a time.
 *
 * CameraX can discard frames before they reach the analyzer, but the analyzer callback
 * launches coroutine work and returns immediately. This gate prevents those coroutine
 * jobs from overlapping or forming an inference backlog.
 */
internal class SingleFlightGate {
    private val processing = AtomicBoolean(false)

    fun tryAcquire(): Boolean = processing.compareAndSet(false, true)

    fun release() {
        check(processing.compareAndSet(true, false)) {
            "SingleFlightGate released without being acquired"
        }
    }
}
