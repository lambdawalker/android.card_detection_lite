# ML Inference & Tracking Pipeline

The ML pipeline in `cardDetectionLite` is engineered for high-throughput, low-latency object detection on mobile hardware. It combines TensorFlow Lite, YOLO post-processing, perceptual hashing, and a temporal state machine.

---

## 1. TFLite Interpreter (`TfliteInterpreter`)

`TfliteInterpreter` encapsulates the raw TensorFlow Lite runtime and handles device-specific execution details.

```
Bitmap Input ──> Pre-allocated ByteBuffer ──> TFLite Interpreter (GPU / CPU) ──> De-quantized Output Floats
```

### Key Technical Capabilities:

1. **Dynamic Tensor Probing**:
   - On initialization, probes model input shapes and output dimensions.
   - Automatically detects YOLO memory layout:
     - `OutputLayout.ATTRS_X_BOXES`: Shape `[1, attributes, boxes]` (e.g., `[1, 85, 8400]`).
     - `OutputLayout.BOXES_X_ATTRS`: Shape `[1, boxes, attributes]` (e.g., `[1, 8400, 85]`).

2. **Quantization Handling**:
   - Supports both `FP32` (floating point) and `INT8` quantized models.
   - For `FP32`: Normalizes pixel values from `[0..255]` to `[0.0..1.0]`.
   - For `INT8`: Quantizes pixels into signed bytes `[-128..127]` using input scale/zero-point and dequantizes outputs using output scale/zero-point.

3. **Zero-Allocation Memory Reuse**:
   - Pre-allocates direct native `ByteBuffer` objects for input and output.
   - Reuses a single `IntArray` pixel buffer (`pixelBuffer`) across frames to eliminate garbage collection pauses during live inference streams.

4. **Hardware Acceleration**:
   - Checks device compatibility via `CompatibilityList` and instantiates `GpuDelegate` for `FP32` models when enabled.
   - Configures CPU thread allocation via `NumThreads` when running on CPU.

---

## 2. YOLO Post-Processor (`YoloPostProcessor`)

`YoloPostProcessor` converts raw tensor float arrays into structured `Detection` objects.

### Pipeline Stages:

1. **Tensor Decoding**:
   - Traverses output boxes, extracting box center coordinates `(cx, cy)`, dimensions `(w, h)`, and class probabilities.
   - Filters out candidates below `scoreThreshold`.
   - Reverses letterbox scaling and padding to restore coordinates to original image dimensions.

2. **Non-Max Suppression (NMS)**:
   - Sorts candidates by confidence score.
   - Calculates Intersection-over-Union (IoU) between overlapping candidates of the same class.
   - Suppresses overlapping boxes exceeding `iouThreshold`, preserving up to `maxNmsCandidates`.

---

## 3. Heuristic Validators (`CardValidator`)

Candidate detections must pass heuristic validation rules before entering the tracking state machine:

- **`MarginValidator`**: Rejects detections positioned within a configurable margin (`margin = 20u`) from image edges to prevent processing truncated cards.
- **`AspectRatioValidator`**: Verifies that bounding box ratio `longestSide / shortestSide` falls within acceptable ID card aspect ratio bounds (`minAspectRatio = 1.28f`, `maxAspectRatio = 1.70f`).

---

## 4. Perceptual Difference Hashing (dHash)

To verify visual consistency without expensive deep-feature extraction, `CardDetector` uses a 64-bit **Difference Hash (dHash)** algorithm (`generateDHashFromRegion`):

1. Downsamples the bounding box region to a 9x8 grid.
2. Converts pixel values to integer luminance:
   $$\text{Luminance} = 30 \times R + 59 \times G + 11 \times B$$
3. Compares horizontal neighbor brightness across the 8x8 grid to generate a 64-bit `ULong` hash.
4. Compares hashes across frames using bitwise Hamming Distance (`ULong.hammingDistanceTo`).

---

## 5. Temporal Tracking State Machine (`CardDetector`)

`CardDetector` maintains spatial and temporal continuity across video frames.

```
[Candidate Detected] ──> Frame 1..N-1 ──> LockingCard (lockOnProgress < 1.0)
                            │
                            ▼
                    Frame N (lockOnThreshold) ──> NewCard (lockOnProgress = 1.0, id assigned)
                            │
                            ▼
                    Frame N+1+ ──> CardLocked (id retained, progress = 1.0)
```

### Locking Progression Criteria:
- **`LockingCard`**: Card candidate detected, progress increments linearly (`candidateConsistencyCount / lockOnThreshold`).
- **`NewCard`**: Threshold reached (e.g., 5 consistent frames). Generates a unique monotonic `card.id` and emits `NewCard` status.
- **`CardLocked`**: Subsequent frames maintain identity `card.id` and `lockOnProgress = 1.0f`.

### Robustness & Auto-Focus Resilience:
- When locked, spatial continuity check (`spatialOverlap >= 0.25` IoU with previous box) preserves the candidate even if camera auto-focus or lighting shifts temporarily alter the dHash distance.
- `noDetectionCountLimit`: Permits up to $N$ consecutive missing frames before clearing tracking state.
- `memoryDetectionTimeLimit`: Resets tracking state if time between detections exceeds limit (e.g., 1000ms).

---

## 6. Preprocessing Transformations (`PreProcessingImageTransformation`)

Configures how input camera bitmaps are cropped prior to inference:

- **`FullImage`**: Analyzes the entire camera field of view with letterboxing.
- **`CenterSquareCrop`**: Crops a centered square region.
- **`SquareCrop(top)`**: Crops a square region with custom vertical offset.
- **`CenterVisibleImage`**: Crops the region matching the visible preview screen aspect ratio.
- **`VisibleImage(top)`**: Visible preview crop with vertical offset.
- **`CenterVisibleImageSquareCrop`**: Center square crop of visible preview region.
- **`VisibleImageSquareCrop(top)`**: Visible square crop with vertical offset.
