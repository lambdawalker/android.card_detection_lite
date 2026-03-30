package com.apexfission.android.carddetectionlite.domain.tflite.image


import android.graphics.Bitmap
import androidx.core.graphics.scale

/**
 * Implements the Difference Hash (dHash) algorithm for perceptual image comparison.
 *
 * dHash is a simple and fast algorithm to generate a "fingerprint" of an image based on
 * gradients between adjacent pixels. It is resilient to minor changes such as scaling,
 * compression artifacts, and small adjustments in brightness or contrast.
 *
 * ### Typical usage:
 * ```kotlin
 * val h1 = bitmap1.generateDHash()
 * val h2 = bitmap2.generateDHash()
 * val distance = h1.hammingDistanceTo(h2)
 * val areSimilar = distance <= 10
 * ```
 */

/**
 * Generates a perceptual hash (dHash) for this [Bitmap].
 *
 * The algorithm works as follows:
 * 1.  Resizes the image to a small, fixed size: `(hashSize + 1) x hashSize`.
 * 2.  Converts the image to grayscale luminance.
 * 3.  Iterates through each row, comparing adjacent pixels.
 * 4.  For each pair, it outputs a `1` if the left pixel is brighter than the right, and `0` otherwise.
 * 5.  The resulting bits are combined to form the final hash.
 *
 * @param hashSize The dimension of the hash. The resulting hash will have `hashSize * hashSize` bits.
 *                 For example, the default `hashSize` of 8 produces a 64-bit hash.
 * @return A [ULong] representing the calculated dHash.
 * @throws IllegalArgumentException if `hashSize` is not between 2 and 16.
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

/**
 * Calculates the Hamming distance between two dHashes.
 *
 * The Hamming distance is the number of bit positions at which the two hashes differ.
 * A smaller distance indicates greater similarity between the images.
 *
 * @param other The other [ULong] hash to compare against.
 * @return The Hamming distance (an `Int`) between the two hashes.
 */
fun ULong.hammingDistanceTo(other: ULong): Int {
    var x = this xor other
    var count = 0
    while (x != 0uL) {
        x = x and (x - 1uL)
        count++
    }
    return count
}

/**
 * A convenience function to calculate the dHash distance between two [Bitmap]s directly.
 *
 * @param a The first bitmap.
 * @param b The second bitmap.
 * @param hashSize The size of the dHash to generate.
 * @return The Hamming distance between the dHashes of the two bitmaps.
 */
fun dhashDistance(a: Bitmap, b: Bitmap, hashSize: Int = 8): Int {
    val ha = a.generateDHash(hashSize)
    val hb = b.generateDHash(hashSize)
    return ha.hammingDistanceTo(hb)
}

/**
 * Determines if two [Bitmap]s are visually similar based on a dHash distance threshold.
 *
 * ### Rule-of-thumb thresholds for an 8x8 dHash:
 * - **0-5**: Near-identical images.
 * - **6-10**: Very similar images (e.g., same image with different compression).
 * - **11-20**: Somewhat similar (e.g., same scene with minor changes).
 * - **21+**: Likely different images.
 *
 * Note: These are general guidelines. Optimal thresholds may vary depending on the specific
 * use case, image content, and environmental factors like lighting.
 *
 * @param a The first bitmap.
 * @param b The second bitmap.
 * @param maxDistance The maximum Hamming distance to be considered "similar".
 * @param hashSize The size of the dHash to generate for the comparison.
 * @return `true` if the dHash distance is less than or equal to `maxDistance`, `false` otherwise.
 */
fun isVisuallySimilar(a: Bitmap, b: Bitmap, maxDistance: Int = 18, hashSize: Int = 8): Boolean {
    return dhashDistance(a, b, hashSize) <= maxDistance
}

/**
 * Determines if two dHashes are similar based on a distance threshold.
 *
 * @param a The first dHash.
 * @param b The second dHash.
 * @param maxDistance The maximum Hamming distance to be considered "similar".
 * @return `true` if the distance is less than or equal to `maxDistance`, `false` otherwise.
 */
fun isVisuallySimilar(a: ULong, b: ULong, maxDistance: Int = 18): Boolean {
    return a.hammingDistanceTo(b) <= maxDistance
}
