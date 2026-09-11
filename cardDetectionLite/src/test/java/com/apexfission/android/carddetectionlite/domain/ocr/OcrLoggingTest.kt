package com.apexfission.android.carddetectionlite.domain.ocr

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OcrLoggingTest {
    @Test
    fun `ocr wrapper never logs recognized text`() {
        val source = locateOcrWrapper().readText()
        val logCalls = Regex("Log\\.[a-zA-Z]+\\([^\\n]+")
            .findAll(source)
            .map { it.value }
            .toList()

        assertFalse(
            "OCR logs must not interpolate recognized identity text",
            logCalls.any { call -> "result.text" in call || "it.text" in call },
        )
    }

    private fun locateOcrWrapper(): File {
        val candidates = listOf(
            File("src/main/java/com/apexfission/android/carddetectionlite/domain/ocr/OcrWrapper.kt"),
            File("cardDetectionLite/src/main/java/com/apexfission/android/carddetectionlite/domain/ocr/OcrWrapper.kt"),
        )
        return candidates.firstOrNull(File::isFile).also {
            assertTrue("Unable to locate OcrWrapper.kt", it != null)
        }!!
    }
}
