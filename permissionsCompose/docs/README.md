# :permissionsCompose documentation

This Android/Compose library supplies camera permission state, explicit actions, and an optional explanatory screen. It can be used independently of card detection and has no project-module dependency.

- [Permission flow](guide.md): controller, gate, recovery, and host responsibilities.
- [Testing](testing.md): status tests and device checks.
- [Package map](packages.md): production and test source.

It uses Accompanist Permissions, Compose, and lifecycle/ViewModel integration; minSdk 26, compileSdk 36, toolchain 17. See [build configuration](../build.gradle.kts).
