package com.apexfission.android.carddetectionlite.ui.camerapreview

import androidx.compose.runtime.Immutable
import com.apexfission.android.carddetectionlite.domain.coordinates.models.ImagePoint
import com.apexfission.android.carddetectionlite.domain.coordinates.models.ImageSpaceChain
import com.apexfission.android.carddetectionlite.domain.coordinates.transformations.toChildSpace
import com.apexfission.android.carddetectionlite.domain.tflite.model.CardDetection
import kotlin.math.abs

/**
 * Represents a 2D focus target point in preview pixel space.
 *
 * @property x The X-coordinate in pixels.
 * @property y The Y-coordinate in pixels.
 */
@Immutable
data class FocusPoint(
    val x: Float,
    val y: Float
)

/** Maps a focus target from the upright source image into preview pixel coordinates. */
internal fun FocusPoint.toPreviewSpace(chain: ImageSpaceChain): FocusPoint? {
    if (chain.isEmpty() || !x.isFinite() || !y.isFinite()) return null
    val sourceSpace = chain.first().space
    if (x < 0f || y < 0f || x > sourceSpace.width.toFloat() || y > sourceSpace.height.toFloat()) {
        return null
    }

    val previewPoint = ImagePoint(x.toUInt(), y.toUInt()).toChildSpace(chain)
    return FocusPoint(previewPoint.x.toFloat(), previewPoint.y.toFloat())
}

/**
 * Result of evaluating [AutoFocusPolicy.shouldTriggerFocus].
 *
 * @property shouldFocus `true` if the camera auto-focus action should be executed.
 * @property focusPoint The calculated [FocusPoint] for auto-focus, or `null` if no focus is needed.
 */
data class AutoFocusResult(
    val shouldFocus: Boolean,
    val focusPoint: FocusPoint? = null
)

/**
 * Decouples smart auto-focus heuristics and decision logic from Compose UI lifecycle events.
 *
 * Evaluates whether a new card detection event warrants triggering camera auto-focus/auto-exposure
 * based on cooldown time, positional movement, and bounding box area changes.
 *
 * @property cooldownMs Minimum time interval in milliseconds between auto-focus actions (default 500ms).
 * @property positionThresholdPx Minimum spatial shift in pixels required to trigger focus (default 50px).
 * @property areaChangeThreshold Minimum percentage change in bounding box area required to trigger focus (default 10%).
 */
class AutoFocusPolicy(
    private val cooldownMs: Long = 500L,
    private val positionThresholdPx: Float = 50f,
    private val areaChangeThreshold: Float = 0.10f
) {
    private var lastFocusArea: Float = 0f
    private var lastFocusTimestamp: Long = 0L
    private var lastCardCoordinates: FocusPoint = FocusPoint(-1000f, -1000f)

    /**
     * Evaluates a [CardDetection] event and determines whether an auto-focus action should be triggered.
     *
     * @param detection The current [CardDetection] event.
     * @param currentTimeMs Current timestamp in milliseconds (defaults to [System.currentTimeMillis]).
     * @return An [AutoFocusResult] indicating whether to focus and the target [FocusPoint].
     */
    fun shouldTriggerFocus(
        detection: CardDetection?,
        currentTimeMs: Long = System.currentTimeMillis()
    ): AutoFocusResult {
        if (detection == null) return AutoFocusResult(shouldFocus = false)

        val cardBox = detection.card.box
        val centerX = (cardBox.x + cardBox.x2).toFloat() / 2f
        val centerY = (cardBox.y + cardBox.y2).toFloat() / 2f
        val currentArea = cardBox.width.toFloat() * cardBox.height.toFloat()

        val isCooldownOver = (currentTimeMs - lastFocusTimestamp) >= cooldownMs

        val deltaX = abs(lastCardCoordinates.x - centerX)
        val deltaY = abs(lastCardCoordinates.y - centerY)

        val hasMovedSignificantly = deltaX > positionThresholdPx || deltaY > positionThresholdPx
        val hasScaleChanged = if (lastFocusArea == 0f) {
            true
        } else {
            val areaChange = abs(currentArea - lastFocusArea) / lastFocusArea
            areaChange > areaChangeThreshold
        }

        if (isCooldownOver && (hasMovedSignificantly || hasScaleChanged)) {
            lastCardCoordinates = FocusPoint(centerX, centerY)
            lastFocusArea = currentArea
            lastFocusTimestamp = currentTimeMs
            val targetPoint = FocusPoint(centerX, centerY)
            return AutoFocusResult(shouldFocus = true, focusPoint = targetPoint)
        }

        return AutoFocusResult(shouldFocus = false)
    }

    /** Resets internal tracking state. */
    fun reset() {
        lastFocusArea = 0f
        lastFocusTimestamp = 0L
        lastCardCoordinates = FocusPoint(-1000f, -1000f)
    }
}
