package com.apexfission.android.carddetectionlite.domain.ocr

import android.graphics.Bitmap
import android.graphics.Rect
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

data class OcrResult(
    val text: String,
    val boundingBox: Rect?
)

class OcrWrapper {
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    fun run(image: Bitmap): List<OcrResult> {
        val inputImage = InputImage.fromBitmap(image, 0)
        val result: Text = Tasks.await(recognizer.process(inputImage))
        return result.textBlocks.map {
            OcrResult(it.text, it.boundingBox)
        }
    }
}