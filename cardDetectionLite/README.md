# cardDetectionLite Module

## Overview

This module provides a real-time, all-in-one Jetpack Compose component for card id cards (e.g., driver’s license, voting id, etc.) detection. It uses
a YOLO (You Only Look Once) object detection model, optimized for TensorFlow Lite, to detect from the camera feed. The model is approximately 5.3 MB.
Its primary purpose is to perform the initial object detection and segmentation, isolating the card from the camera view.

**Note:** This module does **not** perform Optical Character Recognition (OCR). It is designed to be the first step in a pipeline, providing a cropped
image of the card that can then be passed to a separate OCR engine.

The primary entry point is the `CardDetectorLite` composable, which encapsulates the camera preview, model inference, and result visualization into a
single, highly configurable UI component.

## Features

* **Real-time Card Detection**: Leverages a high-performance YOLO model running on-device with TensorFlow Lite for fast and efficient detection.
* **Image Preprocessing**: Automatically prepares input images for the model, including letterboxing or cropping to handle different aspect ratios.
* **Detection Stability (Lock-On)**: Implements a temporal filtering mechanism using perceptual hashing to ensure a card detection is stable across
  multiple frames before it is confirmed. This reduces false positives and flickering results.
* **Feature Extraction**: Beyond just detecting the card, the module can identify and extract other features or elements (e.g., barcodes, pdf417, mrz,
  qr-code, identification photo) located within the boundaries of the detected card.
* **Customizable Validation**: Includes a flexible validation system to filter detections based on custom criteria such as confidence score, size, or
  aspect ratio or position.
* **Smart Auto-Focus**: Intelligently focuses the camera on the detected card based on its size and position, improving image clarity.
* **Pluggable Model Architecture**: Designed to easily switch between different TFLite models, allowing for easy updates and experimentation.

## Configuration

The `CardDetectorLite` composable can be configured with the following parameters:

| Parameter                  | Type                      | Default Value                                       | Description                                                                                                                                                             |
|----------------------------|---------------------------|-----------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `modelPath`                | `String`                  | -                                                   | The path to the `.tflite` model file within the application's `assets` directory.                                                                                       |
| `classLabels`              | `Map<Int, String>`        | -                                                   | A map where keys are the integer class IDs from the model and values are the corresponding human-readable string labels.                                                |
| `cardClasses`              | `List<Int>`               | -                                                   | A specific list of class IDs that should be treated as the primary target for detection and lock-on.                                                                    |
| `isDetectionEnabled`       | `Boolean`                 | -                                                   | A flag to dynamically start or stop the detection process.                                                                                                              |
| `onCardDetection`          | `(CardDetection) -> Unit` | -                                                   | A callback lambda invoked only when the detector achieves a stable "lock-on" on a card.                                                                                 |
| `modifier`                 | `Modifier`                | `Modifier`                                          | A standard Jetpack Compose Modifier applied to the root `Box` of the component.                                                                                         |
| `useGpu`                   | `Boolean`                 | `true`                                              | If `true`, the TFLite interpreter will attempt to use the GPU delegate for potentially faster inference.                                                                |
| `showBoundingBoxes`        | `Boolean`                 | `false`                                             | When `true`, a visualization overlay is displayed, drawing boxes around all detected objects.                                                                           |
| `showClassNames`           | `Boolean`                 | `false`                                             | If `true` (and `showBoundingBoxes` is true), labels with the class name and confidence score will be drawn above each bounding box.                                        |
| `showFlashlightSwitch`     | `Boolean`                 | `true`                                              | If `true`, a UI button is provided to allow the user to toggle the camera's flashlight.                                                                                 |
| `showLockOnProgress`       | `Boolean`                 | `true`                                              | If `true`, an overlay is displayed providing visual feedback as the detector locks onto a card.                                                                         |
| `scoreThreshold`           | `Float`                   | `0.65f`                                             | The minimum confidence score (0.0 to 1.0) a detection must have to be considered.                                                                                       |
| `analysisTargetResolution` | `Size`                    | `Size(2048, 1080)`                                  | The target resolution for the image analysis stream.                                                                                                                    |
| `cardCardFilters`          | `List<CardValidator>`     | `listOf(MarginValidator(), AspectRatioValidator())` | A list of validators to apply additional heuristic checks on potential card detections.                                                                                 |
| `imageMode`                | `InputShape`              | `InputShape.SquareCrop`                             | The method used to preprocess the camera image. See the 'Image Preprocessing Modes' section below for details.                                                          |
| `inferenceIntervalMs`      | `Long`                    | `33L`                                               | The minimum interval, in milliseconds, between consecutive model inferences. Increasing this can reduce power consumption.                                                |
| `tapToFocusEnabled`        | `Boolean`                 | `true`                                              | If `true`, the user can tap on the camera preview to trigger a manual focus event.                                                                                      |
| `focusOnCardEnabled`       | `Boolean`                 | `true`                                              | If `true`, the camera will automatically try to focus on the detected card.                                                                                             |
| `lockOnThreshold`          | `Int`                     | `5`                                                 | The number of consecutive, visually similar frames required before a card detection is considered stable and "locked on."                                               |
| `numThreads`               | `Int?`                    | `null`                                              | The number of threads for CPU inference. If `null`, the default is `max(2, num_processors / 2)`.                                                                      |

### Understanding the Output Card Resolution

The resolution of the final, cropped card image returned by the `onCardDetection` callback is crucial for subsequent processing, such as OCR. This resolution is not fixed and depends on a combination of factors:

1.  **`analysisTargetResolution`**: This parameter sets the resolution of the image that the model analyzes. A higher analysis resolution provides more pixel data, which can lead to a higher-quality cutout. However, this also increases the processing load and may impact performance.

2.  **Physical Distance**: The closer the camera is to the card, the more pixels the card will occupy in the camera's field of view. This is the most significant factor in determining the final resolution. A card that fills a large portion of the screen will naturally result in a much higher-resolution image than a card that is far away.

3.  **`imageMode`**: The preprocessing mode affects what portion of the camera's view is analyzed.
    *   With modes like `FullImage`, the card might only be a small part of the total area being analyzed, which can reduce the effective resolution of the card itself.
    *   Cropping modes like `SquareCrop` can be beneficial if the user is guided to center the card, as it ensures the card takes up a larger percentage of the analyzed image.

**Tip for Best Quality:** For the highest resolution cutout, instruct users to bring the card as close to the camera as possible while ensuring it remains fully within the frame. Combining this with a high `analysisTargetResolution` will yield the best results for any subsequent processing steps.

### Image Preprocessing Modes (`imageMode`)

The `imageMode` parameter controls how the image from the camera is prepared before being sent to the detection model. The choice of strategy can impact which areas of the camera view are analyzed for detection.

*   **`InputShape.FullImage`**
    *   **Description**: Uses the entire image captured by the camera sensor. This is ideal for detecting objects anywhere in the camera's field of view.
    *   **Behavior**: The image is letterboxed (padded) to fit the model's expected aspect ratio, ensuring the entire scene is analyzed without distortion.

*   **`InputShape.SquareCrop`** (Default)
    *   **Description**: Crops the center of the full camera image to a square.
    *   **Behavior**: This is useful when the object of interest (e.g., a card held by the user) is expected to be in the center of the frame. It can improve performance by focusing the model on a specific region.

*   **`InputShape.VisibleImage`**
    *   **Description**: Uses only the portion of the camera image that is currently visible in the on-screen preview. Camera sensors often have a different aspect ratio than the screen, meaning they capture more than what is displayed.
    *   **Behavior**: This mode is useful for ensuring that detections only occur on objects the user can actually see. The visible area is letterboxed to fit the model's input dimensions.

*   **`InputShape.VisibleImageSquareCrop`**
    *   **Description**: Crops the center of the *visible* camera preview to a square.
    *   **Behavior**: This combines the benefits of `VisibleImage` and `SquareCrop`, focusing the detection efforts on the center of the area displayed to the user.
