package com.apexfission.android.carddetectionlite.domain.tflite.image


import android.graphics.Bitmap
import androidx.core.graphics.scale

/**
 * Implements the Difference Hash (dHash) algorithm for fast, perceptual image comparison.
 *
 * dHash creates a "fingerprint" of an image based on the relative brightness of adjacent pixels.
 * This makes it remarkably resilient to changes that don't alter the core structure of the image,
 * such as scaling, minor brightness/contrast adjustments, and compression artifacts. It is ideal
 * for determining if two images are "the same" from a human perspective.
 *
 * ### Typical Usage:
 * ```kotlin
 * val hash1 = bitmap1.generateDHash()
 * val hash2 = bitmap2.generateDHash()
 *
 * val distance = hash1.hammingDistanceTo(hash2)
 *
 * if (distance < 10) {
 *     // Images are considered very similar
 * }
 * ```
 */

/**
 * Generates a perceptual hash (dHash) for this [Bitmap].
 *
 * The algorithm performs the following steps:
 * 1.  **Resize**: The image is downscaled to a small, fixed size (`(hashSize + 1) x hashSize`).
 *     This step removes high-frequency details and normalizes the image dimensions.
 * 2.  **Grayscale**: The color information is discarded by converting pixels to luminance values.
 * 3.  **Compare**: It iterates through each row, comparing the brightness of each pixel to its
 *     immediate right neighbor.
 * 4.  **Hash**: If a pixel is brighter than its neighbor, a `1` bit is recorded; otherwise, a `0` is recorded.
 *     These bits are concatenated to form the final hash.
 *
 * @param hashSize The dimension for the hash calculation. The final hash will have `hashSize * hashSize` bits.
 *                 The value must be between 2 and 8, as the resulting hash is stored in a 64-bit [ULong].
 *                 The default of 8 results in a 64-bit hash.
 * @return A [ULong] representing the calculated dHash of the image.
 * @throws IllegalArgumentException if `hashSize` is outside the supported range of [2, 8].
 */
fun Bitmap.generateDHash(hashSize: Int = 8): ULong {
    require(hashSize in 2..8) { "hashSize must be between 2 and 8" }

    val targetW = hashSize + 1
    val targetH = hashSize

    val scaled = if (this.width == targetW && this.height == targetH) this else this.scale(targetW, targetH)
    val pixels = IntArray(targetW * targetH)
    scaled.getPixels(pixels, 0, targetW, 0, 0, targetW, targetH)
    if (scaled !== this) scaled.recycle()

    var hash = 0uL
    var bitIndex = 0

    for (y in 0 until targetH) {
        val rowOffset = y * targetW
        for (x in 0 until hashSize) {
            val pLeft = pixels[rowOffset + x]
            val pRight = pixels[rowOffset + x + 1]

            // Fast inline luminance calculation: (0.299*R + 0.587*G + 0.114*B) using integer math.
            val lumLeft = (30 * ((pLeft shr 16) and 0xFF) + 59 * ((pLeft shr 8) and 0xFF) + 11 * (pLeft and 0xFF))
            val lumRight = (30 * ((pRight shr 16) and 0xFF) + 59 * ((pRight shr 8) and 0xFF) + 11 * (pRight and 0xFF))

            if (lumLeft > lumRight) {
                hash = hash or (1uL shl bitIndex)
            }
            bitIndex++
        }
    }
    return hash
}

/**
 * Calculates the Hamming distance between two dHashes.
 *
 * The Hamming distance is simply the count of bit positions at which two hashes differ.
 * This value is a direct, numerical measure of how visually different the two source images are.
 * A distance of `0` means the hashes are identical.
 *
 * @param other The other [ULong] hash to compare against this one.
 * @return An `Int` representing the number of differing bits.
 */
fun ULong.hammingDistanceTo(other: ULong): Int {
    // This is a highly optimized bit-counting method.
    return java.lang.Long.bitCount((this xor other).toLong())
}

/**
 * A convenience function to compute the dHash distance between two [Bitmap]s in one step.
 *
 * @param a The first bitmap.
 * @param b The second bitmap.
 * @param hashSize The size of the dHash to generate (e.g., 8 for a 64-bit hash).
 * @return The Hamming distance between the dHashes of the two bitmaps.
 */
fun dhashDistance(a: Bitmap, b: Bitmap, hashSize: Int = 8): Int {
    val ha = a.generateDHash(hashSize)
    val hb = b.generateDHash(hashSize)
    return ha.hammingDistanceTo(hb)
}

/**
 * Determines if two [Bitmap]s are visually similar by comparing their dHash distance
 * against a given tolerance.
 *
 * ### Practical Thresholds (for an 8x8 dHash):
 * - **0-5**:   The images are nearly identical.
 * - **6-10**:  The images are very similar (e.g., the same photo with different watermarks or compression levels).
 * - **11-20**: The images are somewhat similar (e.g., a photo of the same scene from a slightly different angle).
 * - **21+**:   The images are likely different.
 *
 * @param a The first bitmap to compare.
 * @param b The second bitmap to compare.
 * @param maxDistance The maximum Hamming distance that is still considered "similar".
 *                    This parameter acts as the tolerance for the comparison.
 * @param hashSize The size of the dHash to use for the comparison.
 * @return `true` if the calculated Hamming distance is less than or equal to `maxDistance`.
 */
fun isVisuallySimilar(a: Bitmap, b: Bitmap, maxDistance: Int = 18, hashSize: Int = 8): Boolean {
    return dhashDistance(a, b, hashSize) <= maxDistance
}

/**
 * Determines if two pre-calculated dHashes are similar based on a distance threshold.
 *
 * This is a more performant version of the comparison if you have already computed the hashes.
 *
 * @param a The first dHash.
 * @param b The second dHash.
 * @param maxDistance The maximum Hamming distance to be considered "similar".
 * @return `true` if the distance is less than or equal to `maxDistance`.
 */
fun isVisuallySimilar(a: ULong, b: ULong, maxDistance: Int = 18): Boolean {
    return a.hammingDistanceTo(b) <= maxDistance
}
