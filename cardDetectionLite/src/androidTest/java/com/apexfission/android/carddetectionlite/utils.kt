package com.apexfission.android.carddetectionlite

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.test.services.storage.TestStorage
import com.apexfission.android.carddetectionlite.domain.coordinates.models.ImageBox
import com.apexfission.android.carddetectionlite.domain.tflite.model.LockingStatus
import kotlin.math.max
import kotlin.math.min

data class GroundTruth(
    val classId: Int, val cx: Float, val cy: Float, val w: Float, val h: Float
) {
    fun toPixelBox(imageWidth: Int, imageHeight: Int): ImageBox {
        val x1 = ((cx - w / 2f) * imageWidth).coerceAtLeast(0f).toUInt()
        val y1 = ((cy - h / 2f) * imageHeight).coerceAtLeast(0f).toUInt()
        val x2 = ((cx + w / 2f) * imageWidth).coerceAtMost(imageWidth.toFloat()).toUInt()
        val y2 = ((cy + h / 2f) * imageHeight).coerceAtMost(imageHeight.toFloat()).toUInt()
        return ImageBox.from2P(x1, y1, x2, y2)
    }
}

fun parseGroundTruth(text: String): GroundTruth {
    val parts = text.trim().split("\\s+".toRegex())
    require(parts.size >= 5) { "Invalid ground truth format: $text" }
    return GroundTruth(
        classId = parts[0].toInt(),
        cx = parts[1].toFloat(),
        cy = parts[2].toFloat(),
        w = parts[3].toFloat(),
        h = parts[4].toFloat()
    )
}

data class ExpectedFrameData(
    val frameIndex: Int,
    val classId: Int,
    val confidence: Float,
    val id: Long?,
    val lockProgress: Float,
    val lockingStatus: LockingStatus,
    val dHash: ULong,
    val xPct: Float,
    val yPct: Float,
    val x2Pct: Float,
    val y2Pct: Float
) {
    fun toPixelBox(imageWidth: Int, imageHeight: Int): ImageBox {
        val x1 = (xPct * imageWidth).coerceAtLeast(0f).toUInt()
        val y1 = (yPct * imageHeight).coerceAtLeast(0f).toUInt()
        val x2 = (x2Pct * imageWidth).coerceAtMost(imageWidth.toFloat()).toUInt()
        val y2 = (y2Pct * imageHeight).coerceAtMost(imageHeight.toFloat()).toUInt()
        return ImageBox.from2P(x1, y1, x2, y2)
    }
}

fun parseExpectedData(text: String): List<ExpectedFrameData> {
    val lines = text.trim().lines().filter { it.isNotBlank() }
    if (lines.isEmpty()) return emptyList()

    val dataLines = if (lines.first().contains("frame")) lines.drop(1) else lines

    return dataLines.map { line ->
        val parts = line.split(",").map { it.trim() }
        require(parts.size >= 11) { "Invalid expected_data line: $line" }
        ExpectedFrameData(
            frameIndex = parts[0].toInt(),
            classId = parts[1].toInt(),
            confidence = parts[2].toFloat(),
            id = if (parts[3] == "null") null else parts[3].toLong(),
            lockProgress = parts[4].toFloat(),
            lockingStatus = LockingStatus.valueOf(parts[5]),
            dHash = parts[6].toULong(),
            xPct = parts[7].toFloat(),
            yPct = parts[8].toFloat(),
            x2Pct = parts[9].toFloat(),
            y2Pct = parts[10].toFloat()
        )
    }
}

fun calculateIoU(a: ImageBox, b: ImageBox): Float {
    val interX1 = max(a.x, b.x)
    val interY1 = max(a.y, b.y)
    val interX2 = min(a.x2, b.x2)
    val interY2 = min(a.y2, b.y2)

    if (interX2 <= interX1 || interY2 <= interY1) return 0f

    val interArea = (interX2 - interX1).toFloat() * (interY2 - interY1).toFloat()
    val areaA = (a.x2 - a.x).toFloat() * (a.y2 - a.y).toFloat()
    val areaB = (b.x2 - b.x).toFloat() * (b.y2 - b.y).toFloat()

    val unionArea = areaA + areaB - interArea
    return if (unionArea > 0f) interArea / unionArea else 0f
}

fun autoSaveBitmap(bitmap: Bitmap, filename: String) {
    val testStorage = TestStorage()
    val outputStream = testStorage.openOutputFile("$filename.png")

    outputStream.use { out ->
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
    }
}

fun drawBoxesOnBitmap(bitmap: Bitmap, groundTruth: ImageBox, detection: ImageBox): Bitmap {
    val mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
    val canvas = Canvas(mutableBitmap)
    val paint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 8f
    }

    paint.color = Color.BLUE
    canvas.drawRect(groundTruth.x.toFloat(), groundTruth.y.toFloat(), groundTruth.x2.toFloat(), groundTruth.y2.toFloat(), paint)

    paint.apply {
        strokeWidth = 2f
    }

    paint.color = Color.RED
    canvas.drawRect(detection.x.toFloat(), detection.y.toFloat(), detection.x2.toFloat(), detection.y2.toFloat(), paint)

    return mutableBitmap
}