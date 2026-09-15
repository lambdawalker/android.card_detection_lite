# CardDetectionLite

Android card detection and temporal tracking using a YOLO-compatible TFLite model, CameraX, and Jetpack Compose. The repository includes a live-camera sample, recorded-video simulation, optional OCR, and reusable geometry and permission modules.

## Modules and documentation

| Module | Contribution | Documentation |
| --- | --- | --- |
| cardDetectionLite | Inference, tracking, camera/video adapters, Compose overlays, optional OCR | [Core guide](cardDetectionLite/docs/README.md) |
| coordinates | Pixel/normalized geometry and directional transformations | [Geometry guide](coordinates/docs/README.md) |
| permissionsCompose | Host-driven camera permission UX and settings recovery | [Permission guide](permissionsCompose/docs/README.md) |
| tfmodel | Bundled model asset and label catalog | [Model guide](tfmodel/docs/README.md) |
| app | Executable integration sample | [Sample guide](app/docs/README.md) |

Each module has a `docs/README.md` index and `docs/packages.md` map. Each directory containing Kotlin source has its own `README.md`, including test source sets. Namespace-only parent directories are not separate packages.

## Getting started

1. Set up the Android SDK used by the build (compileSdk 36) and Java toolchain 17. The sample requires Android API 28 or newer; Android libraries declare minSdk 26.
2. Hydrate Git LFS assets when cloning so model and video files contain their real data.
3. Follow the [current integration example](cardDetectionLite/docs/integration.md) or build the [sample application](app/docs/guide.md).
4. Read [callback ownership and lifecycle](cardDetectionLite/docs/lifecycle.md) before retaining or dispatching images.

```bash
./gradlew :app:assembleDebug
./gradlew :coordinates:test :cardDetectionLite:testDebugUnitTest :permissionsCompose:testDebugUnitTest
```

Android instrumented tests require a connected device/emulator. See each module's testing guide for coverage and limitations. Publication coordinates are declared in the modules' build files; declaration alone does not establish availability in a remote repository.

## How the pieces fit

Production project dependencies are `app → cardDetectionLite, tfmodel, permissionsCompose`, `tfmodel → cardDetectionLite`, and `cardDetectionLite → coordinates`. Core instrumented tests additionally depend on tfmodel.

Frames are converted/cropped, detected or continued through visual hashing, validated, and tracked to lock-on. Compose overlays consume the resulting state. Applications receive a card cutout with source-frame metadata and own the delivered bitmap. Detection callbacks are bounded and may drop pending results; explicit capture copies the retained best image.

Use the [architecture guide](cardDetectionLite/docs/architecture.md) for tensor contracts and coordinate boundaries, and the [model catalog](tfmodel/docs/guide.md) for the nine class labels. Tracking stability does not establish document authenticity, and detecting text/barcode regions does not decode their contents.

Existing preprocessing illustrations and the demo remain under [cardDetectionLite/docs](cardDetectionLite/docs/README.md).

## Maintaining documentation

Update a package README when its responsibility or ownership contract changes. Update its module guide for API/configuration changes and the package map when packages move. Use current declarations and initializer values when older comments differ. Keep examples aligned with function callbacks, explicit bitmap cleanup, and scoped overlays.

## License

[Apache License 2.0](LICENSE).
