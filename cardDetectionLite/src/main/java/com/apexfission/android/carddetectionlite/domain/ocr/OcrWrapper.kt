package com.apexfission.android.carddetectionlite.domain.ocr

import android.graphics.Bitmap
import android.graphics.Rect
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.tasks.await

data class OcrResult(
    val text: String,
    val boundingBox: Rect?
)

internal interface OcrRecognizer : AutoCloseable {
    suspend fun process(image: Bitmap): List<OcrResult>
}

private class MlKitOcrRecognizer(
    private val recognizer: TextRecognizer =
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS),
) : OcrRecognizer {
    override suspend fun process(image: Bitmap): List<OcrResult> {
        val result = recognizer.process(InputImage.fromBitmap(image, 0)).await()
        return result.textBlocks.map { block ->
            OcrResult(block.text, block.boundingBox)
        }
    }

    override fun close() {
        recognizer.close()
    }
}

/**
 * Owns an ML Kit text recognizer.
 *
 * [run] suspends without blocking its caller's thread. Call [close] when OCR is no longer needed;
 * closing is idempotent, waits to release the recognizer until active calls finish, and rejects new
 * calls with [IllegalStateException].
 */
class OcrWrapper internal constructor(
    private val recognizer: OcrRecognizer,
) : AutoCloseable {
    constructor() : this(MlKitOcrRecognizer())

    private val lifecycleLock = Any()
    private var closed = false
    private var activeRequests = 0
    private var recognizerClosed = false

    suspend fun run(image: Bitmap): List<OcrResult> {
        synchronized(lifecycleLock) {
            check(!closed) { "OcrWrapper is closed" }
            activeRequests += 1
        }

        try {
            return recognizer.process(image)
        } finally {
            releaseRequest()
        }
    }

    override fun close() {
        val closeRecognizer = synchronized(lifecycleLock) {
            if (closed) return
            closed = true
            markRecognizerForCloseIfIdle()
        }
        if (closeRecognizer) recognizer.close()
    }

    private fun releaseRequest() {
        val closeRecognizer = synchronized(lifecycleLock) {
            activeRequests -= 1
            markRecognizerForCloseIfIdle()
        }
        if (closeRecognizer) recognizer.close()
    }

    private fun markRecognizerForCloseIfIdle(): Boolean {
        if (!closed || activeRequests != 0 || recognizerClosed) return false
        recognizerClosed = true
        return true
    }
}
