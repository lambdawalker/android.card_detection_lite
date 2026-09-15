# Lifecycle, threads, and image ownership

[Documentation index](README.md)

## Ownership at boundaries

| Boundary | Owner and cleanup |
| --- | --- |
| CameraPreview onFrame | Receiving consumer closes ImageProxy. The detector ViewModel closes rejected and processed frames. |
| Low-level detector bitmap input | Caller retains the bitmap and keeps it valid until the synchronous call returns. |
| Low-level detector ImageProxy input | Caller closes the proxy; detector code releases temporary conversion bitmaps. |
| LetterboxBuilder output | Builder-owned reusable scratch image; released with the builder. |
| onCardDetection bitmap | Application-owned when callback invocation begins; recycle after final use, including failure. |
| Capture bitmap | Independent application-owned copy; recycle after final use. |
| Retained best image | SDK-owned copy, recycled on replacement or store cleanup. |
| OcrWrapper input | Caller-owned; wrapper does not recycle it. |

`Bitmap.use` from the resource package recycles in `finally`. Work must finish within that block. Launching a coroutine inside the block and returning transfers no ownership and can leave the coroutine using a recycled image. For asynchronous handoff, handle cancellation before execution as well as cleanup after execution.

## Backpressure and callbacks

Camera processing uses a single-flight gate in addition to CameraX backpressure. Frames arriving while a processing job is active are closed rather than queued for inference. Video processing has its own admission/ownership handling.

Detection callback delivery is serial on a library worker dispatcher, separate from inference. There may be one running callback and one pending value. A newer result replaces and recycles an undelivered pending image. Therefore callbacks are not a lossless per-frame event stream. In particular, consumers should not assume they observe every lock-state transition.

The library does not reclaim an image already transferred to application code, even if the callback throws. Explicitly supply consuming or recycling callbacks; default no-op lambdas do not provide application cleanup. UI operations must be dispatched to the main thread by the host.

## Capture semantics

The store accepts the first candidate, a higher-confidence candidate, or a newly locked card with a different ID. Capture returns a copy of that retained image on a worker dispatcher. A missing current detection or detection pause does not by itself clear the store, so capture may return an earlier retained result. The store is cleared when its ViewModel is cleared.

## Engine and component lifetime

Use closeable builders for lower-level detectors and engines. A shared `EngineThreadDispatcher` owns a physical worker; close wrappers borrowing it before closing that owner. Nested calls already on the worker execute inline. Do not infer that `Dispatchers.Default` alone meets GPU thread-affinity requirements.

Compose components obtain ViewModels from the current ViewModelStoreOwner. A stable `instanceKey` plus detector configuration controls identity; leaving composition is not the same as clearing the owner's ViewModelStore. ViewModel cleanup closes detector resources, callback dispatch, and retained images.

Camera provider binding coordinates completion with composition disposal. Video preview releases its player on disposal, but the current `BitmapFrameProcessor.release()` is a no-op: it does not suppress executor callbacks already queued. Consumers of the lower-level simulation APIs must account for this limitation.

## OCR

Close `OcrWrapper` when it is no longer needed. Closure is idempotent, rejects new wrapper calls, and defers recognizer disposal until active wrapper requests finish. It returns text-block results without logging recognized text; it does not parse ID fields or own input images.
