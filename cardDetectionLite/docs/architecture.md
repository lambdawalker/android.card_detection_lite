# Detection architecture

[Documentation index](README.md) · [Package map](packages.md)

## Pipeline

1. `CameraPreview` supplies CameraX ImageProxy frames, or Media3 supplies video bitmaps.
2. The relevant ViewModel admits at most one processing job and applies interval throttling.
3. Image utilities produce an upright image and the selected preprocessing crop.
4. The card detector first tries permitted hash continuation; otherwise YOLO runs letterboxing, tensor inference, confidence filtering, coordinate restoration, and class-aware NMS.
5. Candidate selection applies card classes/validators. The lock state machine checks continuity and advances identity/progress.
6. The ViewModel restores preprocessing crop offsets, publishes sequenced metadata, retains a best-image copy when appropriate, and queues a card cutout for the application.
7. Scoped overlays map source bounds into the preview and animate guidance independently.

The card engine, YOLO engine, and inference engine are separate layers. Lower-level builders can be used without Compose. They are synchronous, closeable APIs; thread confinement does not make them nonblocking for their callers.

## Coordinates and images

YOLO postprocessing reverses letterbox scale/padding to recover coordinates in the image supplied to YOLO. The UI ViewModel then restores preprocessing crop offsets so reported card/features refer to the upright source frame.

The callback bitmap is a card cutout, while metadata boxes remain in source-frame coordinates. Do not use those boxes directly as cutout-local or screen coordinates. Preview drawing uses the `:coordinates` ImageSpaceChain; see the [coordinate guide](../../coordinates/docs/guide.md).

`Detection` holds raw object metadata. `CardDetection` adds card/features, nullable ID, `lockOnProgress`, `LockingStatus`, and `DetectionSource`. Status moves through `LockingCard`, `NewCard`, and `CardLocked`; source distinguishes `Yolo` from `Hash`. Missing detections/time limits can reset tracking. Locking is temporal stability, not document authenticity or successful OCR.

## Supported tensor contract

- Exactly one input and one output tensor.
- Input shape `[1, H, W, 3]`, positive dimensions, with `H == W`.
- Output shape `[1, attributes, boxes]` or `[1, boxes, attributes]`.
- The smaller of the two output dimensions is interpreted as attributes, with at least five attributes and distinct dimensions.
- Attributes are four box coordinates followed by class scores; do not supply an incompatible objectness layout.
- Input/output independently support FLOAT32 or INT8. INT8 requires a finite positive scale and a valid byte-range zero point.
- Element and byte counts must fit supported buffer allocation sizes.

See [validation source](../src/main/java/com/apexfission/android/carddetectionlite/domain/tflite/detector/tflite/validation/TensorContractValidator.kt). A filename advertising FP16 weights does not mean the input/output tensors may be FLOAT16. GPU use is attempted for eligible non-INT8 inputs. Unsupported devices can use CPU, but interpreter creation failures still surface as initialization errors; universal GPU fallback is not guaranteed.

## Extension boundaries

Change assets and labels in `:tfmodel` or provide a compatible host model. Change quality rules through `CardValidator`. Change presentation through the overlay slot. Optional `OcrWrapper` is a separate post-capture facility; neither card detection nor the sample's placeholder OCR functions constitute a complete ID-parsing workflow.
