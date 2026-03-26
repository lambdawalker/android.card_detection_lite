package com.apexfission.android.carddetectionlite.domain.tflite.detector


import android.graphics.Bitmap
import androidx.core.graphics.scale

/**
 * Difference Hash (dHash) for perceptual image comparison.
 *
 * Typical usage:
 *   val h1 = bitmap1.generateDHash()
 *   val h2 = bitmap2.generateDHash()
 *   val dist = h1.hammingDistanceTo(h2)
 *   val similar = dist <= 10
 */

/**
 * Generates a 64-bit dHash (default 8x8). Result is an unsigned 64-bit value.
 *
 * How it works:
 * 1) Resize image to (hashSize + 1) x hashSize.
 * 2) Convert to grayscale luminance.
 * 3) For each row, set a bit when pixel(x) > pixel(x+1).
 */
fun Bitmap.generateDHash(hashSize: Int = 8): ULong {
    require(hashSize in 2..16) { "hashSize must be between 2 and 16" }

    // Resize to (hashSize + 1) x hashSize
    val targetW = hashSize + 1
    val targetH = hashSize

    // Use FILTER=true for better downscaling quality
    val scaled = if (this.width == targetW && this.height == targetH) {
        this
    } else {
        this.scale(targetW, targetH)
    }

    // Read pixels
    val pixels = IntArray(targetW * targetH)
    scaled.getPixels(pixels, 0, targetW, 0, 0, targetW, targetH)

    // Convert to luminance (0..255)
    val lum = IntArray(pixels.size)
    for (i in pixels.indices) {
        val c = pixels[i]
        val r = (c shr 16) and 0xFF
        val g = (c shr 8) and 0xFF
        val b = (c) and 0xFF
        // Rec. 601 luma approximation, integer math
        lum[i] = (299 * r + 587 * g + 114 * b + 500) / 1000
    }

    // Build hash: 1 bit per comparison (hashSize * hashSize bits)
    // We pack into a ULong (up to 64 bits for hashSize=8)
    var hash = 0uL
    var bitIndex = 0

    for (y in 0 until targetH) {
        val rowOffset = y * targetW
        for (x in 0 until hashSize) {
            val left = lum[rowOffset + x]
            val right = lum[rowOffset + x + 1]
            val bit = if (left > right) 1uL else 0uL
            // Place LSB-first (bit 0 is first comparison). Any consistent order works.
            hash = hash or (bit shl bitIndex)
            bitIndex++
        }
    }

    // If we created a temporary bitmap, recycle it
    if (scaled !== this) scaled.recycle()

    return hash
}

/** Hamming distance between two 64-bit hashes. Smaller = more similar. */
fun ULong.hammingDistanceTo(other: ULong): Int {
    var x = this xor other
    var count = 0
    while (x != 0uL) {
        x = x and (x - 1uL)
        count++
    }
    return count
}

/** Convenience: compare two bitmaps by dHash distance. */
fun dhashDistance(a: Bitmap, b: Bitmap, hashSize: Int = 8): Int {
    val ha = a.generateDHash(hashSize)
    val hb = b.generateDHash(hashSize)
    return ha.hammingDistanceTo(hb)
}

/**
 * Rule-of-thumb thresholds (8x8):
 *  0-5   : near-identical
 *  6-10  : very similar
 * 11-20  : somewhat similar
 * 21+    : likely different
 *
 * Tune thresholds per your data (compression, crops, lighting).
 */
fun isVisuallySimilar(a: Bitmap, b: Bitmap, maxDistance: Int = 18, hashSize: Int = 8): Boolean {
    return dhashDistance(a, b, hashSize) <= maxDistance
}

fun isVisuallySimilar(a: ULong, b: ULong, maxDistance: Int = 18): Boolean {
    return a.hammingDistanceTo(b) <= maxDistance
}
