# :coordinates package map

[Module documentation](README.md)

Each directory containing Kotlin source has a local README. Namespace-only parent directories do not define separate packages. Test source sets are listed separately, even when they share a production package name.

## main

| Package | Contribution |
| --- | --- |
| [com.apexfission.android.carddetectionlite.domain.coordinates.models](../src/main/java/com/apexfission/android/carddetectionlite/domain/coordinates/models/README.md) | Defines pixel/normalized geometry and directional image-space chains. |
| [com.apexfission.android.carddetectionlite.domain.coordinates.operations](../src/main/java/com/apexfission/android/carddetectionlite/domain/coordinates/operations/README.md) | Converts normalized points and boxes to pixels within one image space. |
| [com.apexfission.android.carddetectionlite.domain.coordinates.transformations](../src/main/java/com/apexfission/android/carddetectionlite/domain/coordinates/transformations/README.md) | Moves points and boxes between parent/child spaces and across directional chains. |

## test

| Package | Contribution |
| --- | --- |
| [com.apexfission.android.carddetectionlite.domain.coordinates](../src/test/java/com/apexfission/android/carddetectionlite/domain/coordinates/README.md) | Checks geometry primitives and directional chains, including parent/child mappings. |
| [com.apexfission.android.carddetectionlite.domain.coordinates.operations](../src/test/java/com/apexfission/android/carddetectionlite/domain/coordinates/operations/README.md) | Checks normalized point/box conversion to pixels. |
| [com.apexfission.android.carddetectionlite.domain.coordinates.transformations](../src/test/java/com/apexfission/android/carddetectionlite/domain/coordinates/transformations/README.md) | Checks pixel and normalized transformations between spaces. |
