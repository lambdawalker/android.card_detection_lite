# Card Detection Lite — Feature Catalog & Architecture Overview

`CardDetectionLite` is a high-performance, production-ready Android library for real-time ID card detection, spatial tracking, and capture using TensorFlow Lite (TFLite), CameraX, and Jetpack Compose.

---

## 1. High-Level Module Structure

The project is organized into sub-projects to separate ML inference, UI rendering, coordinate transformations, and model assets:

```
CardDetectionLite Project
├── :cardDetectionLite (Main Android Library)
│   ├── domain/tflite/detector/ (TFLite Interpreter, YOLO Post-Processing, Card Detector)
│   ├── domain/tflite/filters/  (Margin & Aspect Ratio Heuristic Validators)
│   ├── domain/tflite/image/    (Letterboxing, Crop/Rotate Operations, 64-bit dHash)
│   ├── domain/tflite/model/    (CardDetection, Detection, Feature, LetterboxResult)
│   ├── ui/camerapreview/       (CameraPreview, AutoFocusPolicy, CameraPreset, FocusPoint)
│   ├── ui/detector/           (CardDetectorLite, ViewModel, Scoped Overlay Scope)
│   ├── ui/overlays/           (AnimatedDetectionCanvas, IdCaptureOverlay, CardLockOnOverlay)
│   └── ui/overlays/draw/      (PathUtils, SegmentBuilders, RoundedSegment)
├── :coordinates (Non-Android Coordinate Transformation Engine)
│   ├── models/                (ImageBox, ImagePoint, ImageSpace, NormImageBox, NormImagePoint)
│   ├── operations/            (normBox, normPoint)
│   └── transformations/       (box, point, src)
├── :tfmodel (Model Assets & Class Definitions)
│   └── ModelCatalog.kt        (TFLite Model path, class labels, card class ID sets)
├── :permissionsCompose (Camera Permission Wrapper)
└── :app (Sample Demo Application & Simulation Suite)
```

---

## 2. Core Feature Taxonomy

| Feature Category | Key Component(s) | Description |
| :--- | :--- | :--- |
| **All-in-One Composable** | `CardDetectorLite` | Bundles live `CameraX` feed, background TFLite inference, focus management, coordinate transformation, and control overlay. |
| **ML Inference Engine** | `TfliteInterpreter` | High-performance TFLite wrapper with dynamic tensor probing, direct `ByteBuffer` reuse, INT8/FP32 support, and GPU delegation. |
| **YOLO Post-Processor** | `YoloPostProcessor` | Decodes tensor outputs, applies letterbox un-padding, and executes Non-Max Suppression (NMS) with IoU thresholding. |
| **Spatial Tracking Engine** | `CardDetector` | Temporal state machine tracking cards across frames. Manages lock-on progression (`LockingCard`, `NewCard`, `CardLocked`) and visual consistency via dHash. |
| **Coordinate Transformation** | `:coordinates` module | Immutable coordinate transformation chain mapping normalized/bitmap coordinates to preview screen pixels. |
| **Camera Control & Focus** | `CameraPreview` / `AutoFocusPolicy` | Smart camera auto-focus triggered by spatial movement or area changes. Automatic torch capability probing and state management. |
| **Scoped Overlay Slot API** | `CardDetectorOverlayScope` | Scoped API exposing detection state, latest valid detection, flashlight status, and user intents (`capture`, `goBack`, `toggleFlashlight`). |
| **Animation Engine** | `AnimatedDetectionCanvas` | Shared Canvas and bounds engine animating tracking coordinates (`left`, `top`, `right`, `bottom`), `opacity`, `lockOnProgress`, `smoothProgress`, `breathe`, and `sweepPhase`. |
| **Built-in Overlays** | `IdCaptureOverlay`, `CardLockOnOverlay`, `DebugOverlay`, `DetectionOverlay` | Production-grade ID capture UI, glowing lock-on progress frame, bounding box viewer, and diagnostic panel. |
| **Simulation Suite** | `CardTrackingSimulator` | Offline video frame simulator for testing YOLO card detection on MP4 recordings without physical camera hardware. |

---

## 3. Related Documentation Files

- [ML Inference & Tracking Pipeline](ml_inference_and_tracking_pipeline.md)
- [Camera Preview & Coordinate Transformation System](camera_preview_and_coordinate_system.md)
- [UI Overlays & Scoped Slot Architecture](ui_overlays_and_slot_api.md)
- [Presets & Simulation Suite](simulation_and_presets.md)
