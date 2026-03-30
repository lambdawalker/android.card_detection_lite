package com.apexfission.android.carddetectionlite.domain.tflite.detector

/**
 * Defines strategies for preparing the camera image input for the object detection model.
 *
 * Each option specifies how the image from the camera should be cropped and scaled before
 * inference. The choice of input shape affects the detection area (e.g., the full scene vs.
 * the center), which can in turn impact model performance and accuracy.
 */
enum class InputShape {
    /**
     * Uses the full image from the camera sensor as input, without cropping.
     *
     * This is ideal for detecting objects anywhere in the camera's field of view. The image
     * is letterboxed before being fed to the model to prevent aspect ratio distortion.
     */
    FullImage,

    /**
     * A square crop of the center of the image is used as input.
     *
     * This option crops the center of the image to a square shape. This is useful when the
     * objects of interest are expected to be in the center of the image.
     */
    SquareCrop,

    /**
     * Uses the portion of the image visible in the camera preview as input.
     *
     * This is useful for detecting objects only within the displayed area, as the camera sensor
     * often captures a larger image than what is shown on screen.
     */
    VisibleImage,

    /**
     * A square crop of the center of the visible portion of the image is used as input.
     *
     * This option is useful when objects of interest are expected to be in the center of the
     * visible area of the camera preview.
     */
    VisibleImageSquareCrop
}