# Android Card Detection Lite Audit Remediation Campaign

## Purpose

This campaign resolves the remaining actionable findings from the library audit while keeping every finding independently reviewable and mergeable. Each open finding receives its own branch, focused tests, and a minimal fix. Findings that are already resolved on the branch's base are verified and recorded rather than represented by empty branches.

The source of truth for implementation is the code on the latest remote `main`, not the README or the historical audit line numbers. The audit describes the risk and acceptance criteria; implementation locations may move as earlier fixes merge.

## Current Baseline

At the time this design was written, `origin/main` is commit `8311bbdbe16f3e98140a0be1e5cc10aa7f30dc56`.

| Audit finding | Status | Treatment |
| --- | --- | --- |
| 1. Simulator frame bitmap leak | Merged | Verify only |
| 2. Global letterbox buffer | Merged | Verify only |
| 3. Shared inference output | Merged | Verify only |
| 4. Negative model coordinates | Merged | Verify only |
| 5. Composable ViewModel identity | Published but not merged | Preserve its existing branch; merge before dependent Compose work |
| 7. Rotation bitmap leak | Merged | Verify only |
| 8. `ImageProxy` bitmap leak | Merged | Verify only |
| 44. No embedded credentials detected | Informational | No code branch |

All other findings remain candidates. Before creating each branch, the current code is rechecked because the maintainer has already addressed some smaller findings.

## Branch and Merge Model

Branches use `fix/audit-XX-short-description`. Each independent branch starts from the latest `origin/main`. A branch starts from the immediately preceding finding only when the fix genuinely depends on an unmerged API or invariant introduced there. These stacked dependencies are documented in the branch description and must merge in order.

One finding maps to one branch and one focused commit series. A branch must not absorb unrelated cleanup. If one code change resolves multiple findings inseparably, the first branch contains the implementation and later findings are closed with verification evidence instead of duplicate or empty branches.

The existing `fix/composable-detector-viewmodel-key` branch remains the branch for finding 5. It is not recreated under the new naming convention.

## Workstreams and Merge Order

The order below minimizes conflicts and establishes safety primitives before their consumers.

### OCR and privacy

1. Finding 6 — prevent full identity text from being logged.
2. Finding 20 — give OCR an explicit close lifecycle and replace blocking task waits with cancellation-aware suspension.

Finding 6 is an immediate data-exposure fix. Finding 20 follows because it changes OCR execution and lifecycle behavior.

### CameraX lifecycle and focus

1. Finding 9 — stop global `unbindAll` from removing host-owned use cases.
2. Finding 10 — prevent asynchronous provider completion from binding after disposal.
3. Finding 11 — avoid reusing a shutdown analysis executor.
4. Finding 12 — transform autofocus coordinates into the camera metering space.
5. Finding 31 — include autofocus enablement in effect identity.
6. Finding 32 — keep target rotation synchronized with display changes.
7. Finding 33 — route tap focus through `performClick` for accessibility.
8. Finding 35 — narrow broad `Throwable` catches and preserve cancellation/fatal failures.

These branches are stacked only where an earlier lifecycle abstraction is required. Otherwise they are rebased on the newly merged `main` after each predecessor lands.

### Detector lifecycle, capture state, and model safety

1. Finding 13 — make detector enabled/closed state concurrency-safe.
2. Finding 14 — serialize or atomically protect tracking state.
3. Finding 15 — prevent retained captures from leaking into later sessions.
4. Finding 16 — keep latest-best replacement within the same document identity.
5. Finding 17 — move expensive bitmap copying off the caller/main thread without weakening ownership transfer.
6. Finding 18 — make TensorFlow Lite construction exception-safe.
7. Finding 19 — close model descriptors, streams, and channels deterministically.
8. Finding 25 — validate tensor shapes and data types before inference.
9. Finding 34 — define and enforce callback dispatcher behavior.
10. Finding 36 — remove or gate per-frame detector logging.

Each callback receives an independent bitmap and assumes full ownership at invocation. The SDK never recycles a delivered bitmap. Internal capture retention stays SDK-owned, uses a separate bitmap instance, and never exposes that retained instance directly.

### Coordinate and geometry safety

1. Finding 21 — calculate independent X and Y scale factors.
2. Finding 22 — prevent unsigned underflow in centered crops.
3. Finding 23 — reject or clamp negative signed points before unsigned conversion.
4. Finding 24 — use checked or widened arithmetic for image-box calculations.

These fixes should converge on signed or floating-point intermediate calculations, explicit bounds validation, and conversion to unsigned values only at validated API boundaries.

### Compose overlay behavior

Finding 5 must be merged first.

1. Finding 26 — remove state mutation from the draw phase.
2. Finding 27 — count repeated null detections as tracking misses.
3. Finding 28 — refresh animation behavior when configuration changes.
4. Finding 29 — stop infinite animations while the overlay is invisible.
5. Finding 30 — remove per-draw allocations and logging from hot paths.

State transitions belong in effects or state holders; drawing remains a pure consumer of snapshot state.

### Simulator lifecycle

1. Finding 37 — either honor the viewport size or remove the misleading input.
2. Finding 38 — cancel or invalidate queued callbacks when the simulator is released.
3. Finding 39 — use overflow-safe capture-interval arithmetic.

### Integration and supply-chain hardening

1. Finding 40 — make permission request and denial states explicit and non-looping.
2. Finding 41 — avoid declaring camera hardware mandatory unless the library contract requires it.
3. Finding 42 — replace sample backup placeholders with an explicit safe policy.
4. Finding 43 — move AndroidX JUnit out of production dependencies.
5. Finding 45 — generate an SBOM and add dependency vulnerability scanning in CI.

Finding 45 covers the audit's incomplete advisory visibility; it does not claim that dependency scanners prove the absence of vulnerabilities.

## Per-Finding Workflow

For every candidate finding:

1. Fetch `origin/main` and inspect the current implementation and relevant tests.
2. Decide whether the finding is still reproducible. If already fixed, record the code and test evidence and do not create an empty branch.
3. Create an isolated worktree and the designated branch from the correct base.
4. Add a regression test that demonstrates the unsafe behavior or the required invariant. Where Android framework constraints prevent a local unit test, add the smallest suitable instrumentation, Robolectric, lint, or static verification.
5. Implement the narrowest complete fix, including lifecycle cleanup and public documentation when ownership or threading contracts change.
6. Run the most specific tests first, then module-level checks, formatting/static checks, and the broader available suite.
7. Review the diff for unrelated changes, binary artifacts, debug logging, broad exception handling, and accidental API breakage.
8. Commit and publish the branch, then compare it against its declared base to verify the remote contents.

No work is performed in the maintainer's dirty checkout.

## API and Behavior Decisions

Public API compatibility is preserved by default. A breaking signature or semantic change requires a written rationale before implementation. New lifecycle APIs must be idempotent where practical and document which party owns each closeable or recyclable resource.

Threading is explicit at public boundaries. Callbacks either execute on a documented dispatcher or are marshalled consistently. Coroutine cancellation is never converted into a normal failure result. Resource cleanup remains deterministic on success, failure, and cancellation.

Camera ownership is local: the library unbinds only use cases it created. Detector ownership is instance-local: mutable inference, tracking, bitmap, and lifecycle state is not shared across detector instances unless a synchronization contract covers the entire use.

## Verification Strategy

Each branch includes the strongest practical combination of:

- JVM unit tests for arithmetic, state machines, synchronization helpers, and ownership contracts.
- Instrumented or Compose tests for Android lifecycle, bitmap, CameraX, and drawing behavior.
- Static dependency and manifest checks for integration findings.
- CI SBOM generation and advisory scanning for finding 45.

The current local environment cannot download the uncached Gradle 9.5 distribution because the Gradle service is unreachable. Until a runner with the wrapper available executes the suite, local verification is limited to source inspection, focused checks that do not require the missing distribution, `git diff --check`, and remote commit comparison. Published branches must clearly report this limitation and must not be described as fully passing until CI or an equivalent runner confirms it.

## Completion Criteria

The campaign is complete when every actionable audit finding is either:

- merged with a regression test and verified implementation;
- verified as already resolved, with concrete code/test evidence; or
- explicitly deferred by the maintainer with a documented reason.

Every published branch must have a known base, a focused diff, verification results, and a merge-order note when stacked. Finding 44 remains informational. The final campaign report maps all 45 findings to their disposition and identifies any residual testing or deployment risk.
