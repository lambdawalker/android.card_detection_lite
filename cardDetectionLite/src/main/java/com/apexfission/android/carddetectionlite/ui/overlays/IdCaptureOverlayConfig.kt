package com.apexfission.android.carddetectionlite.ui.overlays

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * Configuration model for the built-in ID capture overlay ([IdCaptureOverlay]).
 *
 * Configures guide opacity, reset thresholds, animation durations, guide smoothing,
 * capture behavior, and display text.
 *
 * @property idleOpacity Opacity of the card guide when no card is detected. Defaults to 0.35f (35%).
 * @property detectedOpacity Opacity of the card guide when a card is detected. Defaults to 0.85f (85%).
 * @property missingCardResetDelayMs Time in milliseconds without a card detection before the guide resets to center. Defaults to 2,000ms.
 * @property maxConsecutiveMisses Optional threshold for consecutive frames without a detection before resetting to center.
 * @property trackingAnimationDurationMs Duration in milliseconds for guide position/size animation while tracking. Defaults to 200ms.
 * @property resetAnimationDurationMs Duration in milliseconds for guide reset animation back to center. Defaults to 300ms.
 * @property fadeAnimationDurationMs Duration in milliseconds for opacity fade animation when card is missing. Defaults to 1,000ms.
 * @property enableGuideSmoothing Whether to smooth bounding box coordinates to prevent jitter. Defaults to true.
 * @property guideSmoothingFactor Smoothing factor between 0f and 1f for coordinate interpolation when smoothing is enabled. Defaults to 0.2f.
 * @property requiresCardDetectionForCapture When true, capture button is disabled until at least one valid card detection exists. Defaults to true.
 * @property title Header text displayed at the top of the overlay. Defaults to "Verify Your Identity".
 * @property instructionTitle Main instruction title. Defaults to "Position your ID within the frame".
 * @property instructionSubTitle Sub-instruction text. Defaults to "We'll use this to pre-fill your information securely".
 * @property guideColor Accent color for the card guide frame. Defaults to bright blue (0xFF2979FF).
 */
@Immutable
data class IdCaptureOverlayConfig(
    val idleOpacity: Float = 0.35f,
    val detectedOpacity: Float = 0.85f,
    val missingCardResetDelayMs: Long = 2_000L,
    val maxConsecutiveMisses: Int? = null,
    val trackingAnimationDurationMs: Int = 200,
    val resetAnimationDurationMs: Int = 300,
    val fadeAnimationDurationMs: Int = 1_000,
    val enableGuideSmoothing: Boolean = true,
    val guideSmoothingFactor: Float = 0.2f,
    val requiresCardDetectionForCapture: Boolean = true,
    val title: String = "Verify Your Identity",
    val instructionTitle: String = "Position your ID within the frame",
    val instructionSubTitle: String = "We'll use this to pre-fill your information securely",
    val guideColor: Color = Color(0xFF2979FF)
)
