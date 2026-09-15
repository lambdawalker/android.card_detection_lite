# `com.apexfission.android.carddetectionlite.ui.overlays.draw`

Source set: `main` · Module: [`:cardDetectionLite`](../../../../../../../../../../docs/README.md)

## Contribution

Provides rounded-corner and connector geometry with glow/blur drawing helpers.

## Responsibilities and boundaries

RoundedSegment, segment builders, and path utilities operate on Compose drawing geometry. Callers supply transformed bounds; this package does not map camera coordinates or manage animation state.

## Source files

- [PathUtils.kt](PathUtils.kt)
- [RoundedSegment.kt](RoundedSegment.kt)
- [SegmentBuilders.kt](SegmentBuilders.kt)
