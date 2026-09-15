# :tfmodel documentation

This Android asset module packages the bundled Sentinel card model and its label catalog. It depends on `:cardDetectionLite` to extend ModelCatalog; the core library's production dependency does not point back to this module.

- [Asset and catalog guide](guide.md): exact asset path, labels, imports, and replacement constraints.
- [Validation](testing.md): packaging and device inference.
- [Package map](packages.md): the catalog extension package.

The module has minSdk 26, compileSdk 36, toolchain 17, and disables compression for tflite assets. See [build configuration](../build.gradle.kts).
