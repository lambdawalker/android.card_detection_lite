package com.apexfission.android.carddetectionlite.ui.camerapreview

/** Coordinates asynchronous camera-provider completion with composition disposal. */
internal class CameraProviderSession {
    private val lock = Any()
    private var disposed = false
    private var bindingCleanup: (() -> Unit)? = null

    fun isActive(): Boolean = synchronized(lock) { !disposed }

    /**
     * Registers cleanup for a successful binding.
     *
     * Returns `false` and runs [cleanup] immediately when disposal won the race.
     */
    fun completeBinding(cleanup: () -> Unit): Boolean {
        val cleanImmediately = synchronized(lock) {
            if (disposed) {
                true
            } else {
                check(bindingCleanup == null) { "Camera binding is already registered" }
                bindingCleanup = cleanup
                false
            }
        }

        if (cleanImmediately) cleanup()
        return !cleanImmediately
    }

    fun dispose() {
        val cleanup = synchronized(lock) {
            if (disposed) return
            disposed = true
            bindingCleanup.also { bindingCleanup = null }
        }
        cleanup?.invoke()
    }
}
