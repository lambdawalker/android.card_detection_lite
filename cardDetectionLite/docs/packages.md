# :cardDetectionLite package map

[Module documentation](README.md)

Each directory containing Kotlin source has a local README. Namespace-only parent directories do not define separate packages. Test source sets are listed separately, even when they share a production package name.

## main

| Package | Contribution |
| --- | --- |
| [com.apexfission.android.carddetectionlite.domain](../src/main/java/com/apexfission/android/carddetectionlite/domain/README.md) | Provides the model-catalog extension point so trained assets can live outside the core library. |
| [com.apexfission.android.carddetectionlite.domain.ocr](../src/main/java/com/apexfission/android/carddetectionlite/domain/ocr/README.md) | Adds optional on-device Latin text recognition after capture. |
| [com.apexfission.android.carddetectionlite.domain.tflite.detector.card.engine](../src/main/java/com/apexfission/android/carddetectionlite/domain/tflite/detector/card/engine/README.md) | Combines object detection, candidate selection, and temporal tracking behind CardDetector. |
| [com.apexfission.android.carddetectionlite.domain.tflite.detector.card.selection](../src/main/java/com/apexfission/android/carddetectionlite/domain/tflite/detector/card/selection/README.md) | Chooses the primary card from raw YOLO detections. |
| [com.apexfission.android.carddetectionlite.domain.tflite.detector.card.tracking](../src/main/java/com/apexfission/android/carddetectionlite/domain/tflite/detector/card/tracking/README.md) | Maintains card identity, lock progress, missing-frame recovery, and visual continuity. |
| [com.apexfission.android.carddetectionlite.domain.tflite.detector.card.transformation](../src/main/java/com/apexfission/android/carddetectionlite/domain/tflite/detector/card/transformation/README.md) | Defines which region of an upright frame is submitted to detection. |
| [com.apexfission.android.carddetectionlite.domain.tflite.detector.tflite.buffers](../src/main/java/com/apexfission/android/carddetectionlite/domain/tflite/detector/tflite/buffers/README.md) | Converts RGB pixels to reusable interpreter input storage and decodes output storage. |
| [com.apexfission.android.carddetectionlite.domain.tflite.detector.tflite.engine](../src/main/java/com/apexfission/android/carddetectionlite/domain/tflite/detector/tflite/engine/README.md) | Owns the TFLite interpreter, optional GPU delegate, inference timing, and physical worker thread. |
| [com.apexfission.android.carddetectionlite.domain.tflite.detector.tflite.validation](../src/main/java/com/apexfission/android/carddetectionlite/domain/tflite/detector/tflite/validation/README.md) | Rejects incompatible tensor contracts before reusable buffers are allocated. |
| [com.apexfission.android.carddetectionlite.domain.tflite.detector.yolo.engine](../src/main/java/com/apexfission/android/carddetectionlite/domain/tflite/detector/yolo/engine/README.md) | Turns upright images into generic object detections through letterboxing, inference, and decoding. |
| [com.apexfission.android.carddetectionlite.domain.tflite.detector.yolo.postprocess](../src/main/java/com/apexfission/android/carddetectionlite/domain/tflite/detector/yolo/postprocess/README.md) | Decodes YOLO predictions into bounded pixel-space detections and removes duplicates. |
| [com.apexfission.android.carddetectionlite.domain.tflite.filters](../src/main/java/com/apexfission/android/carddetectionlite/domain/tflite/filters/README.md) | Supplies pluggable quality rules between detection and candidate acceptance. |
| [com.apexfission.android.carddetectionlite.domain.tflite.image](../src/main/java/com/apexfission/android/carddetectionlite/domain/tflite/image/README.md) | Implements upright frame conversion, preprocessing crops, reusable letterboxing, and perceptual hashing. |
| [com.apexfission.android.carddetectionlite.domain.tflite.model](../src/main/java/com/apexfission/android/carddetectionlite/domain/tflite/model/README.md) | Defines values shared by inference, tracking, view models, and overlays. |
| [com.apexfission.android.carddetectionlite.resource](../src/main/java/com/apexfission/android/carddetectionlite/resource/README.md) | Provides scoped bitmap cleanup for consumers that complete processing inside a block. |
| [com.apexfission.android.carddetectionlite.ui.camerapreview](../src/main/java/com/apexfission/android/carddetectionlite/ui/camerapreview/README.md) | Connects CameraX preview and analysis to Compose lifecycle, focus, and torch controls. |
| [com.apexfission.android.carddetectionlite.ui.detector](../src/main/java/com/apexfission/android/carddetectionlite/ui/detector/README.md) | Provides live-camera Compose integration and coordinates inference, state, capture, and callbacks. |
| [com.apexfission.android.carddetectionlite.ui.overlays](../src/main/java/com/apexfission/android/carddetectionlite/ui/overlays/README.md) | Provides detection, lock-on, debug, and ID-capture UI over scoped detector state. |
| [com.apexfission.android.carddetectionlite.ui.overlays.animation](../src/main/java/com/apexfission/android/carddetectionlite/ui/overlays/animation/README.md) | Exposes animated detection bounds and a Canvas scope for custom drawing. |
| [com.apexfission.android.carddetectionlite.ui.overlays.animation.state](../src/main/java/com/apexfission/android/carddetectionlite/ui/overlays/animation/state/README.md) | Derives remembered guide transitions and animated coordinates, opacity, progress, and optional continuous effects. |
| [com.apexfission.android.carddetectionlite.ui.overlays.draw](../src/main/java/com/apexfission/android/carddetectionlite/ui/overlays/draw/README.md) | Provides rounded-corner and connector geometry with glow/blur drawing helpers. |
| [com.apexfission.android.carddetectionlite.ui.simulation](../src/main/java/com/apexfission/android/carddetectionlite/ui/simulation/README.md) | Runs card tracking on Media3/ExoPlayer video frames instead of a live camera. |

## test

| Package | Contribution |
| --- | --- |
| [com.apexfission.android.carddetectionlite.domain.ocr](../src/test/java/com/apexfission/android/carddetectionlite/domain/ocr/README.md) | Checks OCR lifecycle, cancellation, and absence of recognized-text logging. |
| [com.apexfission.android.carddetectionlite.domain.tflite.detector.card.engine](../src/test/java/com/apexfission/android/carddetectionlite/domain/tflite/detector/card/engine/README.md) | Checks serialized tracking, dedicated-thread use, dHash continuation, and YOLO fallback. |
| [com.apexfission.android.carddetectionlite.domain.tflite.detector.card.tracking](../src/test/java/com/apexfission/android/carddetectionlite/domain/tflite/detector/card/tracking/README.md) | Checks lock progression, missing-detection reset, and hash continuation. |
| [com.apexfission.android.carddetectionlite.domain.tflite.detector.tflite.buffers](../src/test/java/com/apexfission/android/carddetectionlite/domain/tflite/detector/tflite/buffers/README.md) | Checks pixel normalization, INT8 quantization, and output decoding. |
| [com.apexfission.android.carddetectionlite.domain.tflite.detector.tflite.engine](../src/test/java/com/apexfission/android/carddetectionlite/domain/tflite/detector/tflite/engine/README.md) | Checks thread confinement, shared-dispatcher composition, close races, and ownership. |
| [com.apexfission.android.carddetectionlite.domain.tflite.detector.tflite.validation](../src/test/java/com/apexfission/android/carddetectionlite/domain/tflite/detector/tflite/validation/README.md) | Checks accepted tensor layouts and rejected shapes, types, quantization, and allocation overflow. |
| [com.apexfission.android.carddetectionlite.domain.tflite.detector.yolo.postprocess](../src/test/java/com/apexfission/android/carddetectionlite/domain/tflite/detector/yolo/postprocess/README.md) | Checks IoU, class-aware NMS, and invalid/out-of-bounds prediction handling. |
| [com.apexfission.android.carddetectionlite.domain.tflite.filters](../src/test/java/com/apexfission/android/carddetectionlite/domain/tflite/filters/README.md) | Checks aspect-ratio limits, margins, and custom validator composition. |
| [com.apexfission.android.carddetectionlite.domain.tflite.image](../src/test/java/com/apexfission/android/carddetectionlite/domain/tflite/image/README.md) | Checks crop strategies, hash comparison, letterbox ownership, and serialized preprocessing. |
| [com.apexfission.android.carddetectionlite.resource](../src/test/java/com/apexfission/android/carddetectionlite/resource/README.md) | Checks recycling after successful and throwing bitmap consumers. |
| [com.apexfission.android.carddetectionlite.ui](../src/test/java/com/apexfission/android/carddetectionlite/ui/README.md) | Checks presets, overlay scope actions, animated bounds, and continuous-animation policy. |
| [com.apexfission.android.carddetectionlite.ui.camerapreview](../src/test/java/com/apexfission/android/carddetectionlite/ui/camerapreview/README.md) | Checks focus policy, preview coordinate mapping, and binding/disposal races. |
| [com.apexfission.android.carddetectionlite.ui.detector](../src/test/java/com/apexfission/android/carddetectionlite/ui/detector/README.md) | Checks component identity, single-flight admission, frame sequences, retained capture ownership, and bounded callback delivery. |
| [com.apexfission.android.carddetectionlite.ui.simulation](../src/test/java/com/apexfission/android/carddetectionlite/ui/simulation/README.md) | Checks exactly-once recycling and bitmap ownership in accepted/rejected executor dispatch. |

## androidTest

| Package | Contribution |
| --- | --- |
| [com.apexfission.android.carddetectionlite](../src/androidTest/java/com/apexfission/android/carddetectionlite/README.md) | Exercises real model detection and tracking against image/video fixtures, expected boxes, and lock progression; includes generated-image helpers. |
| [com.apexfission.android.carddetectionlite.domain.tflite.detector.tflite.engine](../src/androidTest/java/com/apexfission/android/carddetectionlite/domain/tflite/detector/tflite/engine/README.md) | Exercises real interpreter output independence, CPU/GPU thread affinity, and shared pipeline builders. |
| [com.apexfission.android.carddetectionlite.ui](../src/androidTest/java/com/apexfission/android/carddetectionlite/ui/README.md) | Checks camera preset values on Android. |
