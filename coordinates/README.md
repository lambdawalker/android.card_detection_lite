# :coordinates Module

`coordinates` is a pure Kotlin JVM library module providing spatial primitives, bounding box representations, and resolution chain transformation tools (`ImageSpaceChain`).

It serves as the geometric foundation for mapping coordinates between raw camera sensor frames, preprocessed TFLite crop inputs, and Compose display viewports.

---

## Key Primitives

### 1. `ImageSpace`
Immutable value class representing 2D dimensions, scale factors, and translation offsets for a coordinate system:
- `width: UInt`, `height: UInt`
- `xScale: Float`, `yScale: Float`
- `xOffset: UInt`, `yOffset: UInt`

### 2. `ImagePoint` & `NormImagePoint`
- **`ImagePoint`**: Point in absolute pixel coordinates `(x: UInt, y: UInt)`.
- **`NormImagePoint`**: Point in normalized `[0.0..1.0]` floating-point coordinates `(x: Float, y: Float)`.

### 3. `ImageBox` & `NormImageBox`
Immutable bounding boxes with guaranteed top-left `(x, y)` and bottom-right `(x2, y2)` orientation:
- **Builders**:
  - `ImageBox.from2P(x, y, x2, y2)`: Two-point bounding box construction.
  - `ImageBox.fromPS(x, y, width, height)`: Point and size bounding box construction.
  - `NormImageBox.from2P(x, y, x2, y2)` / `NormImageBox.fromPS(x, y, width, height)`.

---

## Transformations & `ImageSpaceChain`

An **`ImageSpaceChain`** (`List<ImageSpace>`) defines a hierarchical sequence of coordinate spaces (e.g., `[SensorSpace, ScaledSpace, CroppedViewportSpace]`).

### Spatial Transformations
- **`toParentSpace(point, parentSpace)`**: Converts a point from child space coordinates up to parent space coordinates.
- **`toChildSpace(point, childSpace)`**: Converts a point from parent space coordinates down to child space coordinates.
- **`normBoxToBox(targetSpace)`**: Denormalizes a `NormImageBox` `[0..1]` into absolute pixel `ImageBox` coordinates for a target space.
- **`cropAtCenter(targetWidth, targetHeight)`**: Calculates center-crop spatial offsets.
- **`scale(scaleFactor)`**: Produces a scaled `ImageSpace`.

---

## Integration & Dependencies

### `build.gradle.kts`
```kotlin
dependencies {
    implementation(project(":coordinates"))
}
```

---

## Code Example

```kotlin
// Define source video space and display viewport space
val sourceSpace = ImageSpace(width = 1920U, height = 1080U)
val scaledSpace = sourceSpace.scale(1.5f)
val croppedSpace = scaledSpace.cropAtCenter(targetWidth = 1080U, targetHeight = 1080U)

val spaceChain: ImageSpaceChain = listOf(sourceSpace, scaledSpace, croppedSpace)

// Transform a normalized bounding box from TFLite output to viewport pixels
val normBox = NormImageBox.from2P(0.1f, 0.2f, 0.8f, 0.7f)
val displayBox: ImageBox = normBox.normBoxToBox(croppedSpace)
```

---

## Testing

```bash
# Run unit tests
./gradlew :coordinates:test
```
