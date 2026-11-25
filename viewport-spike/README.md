# Viewport spike

The Phase 1 check that the editor viewport works: Ease's triangle drawn with Ease's own wgpu bindings on the device a Stretch `GpuSurface` owns, inside a Stretch `Viewport`, with Compose drawing a box over the triangle and readouts in the corners. The editor replaces it; nothing ships from here.

```
./gradlew :viewport-spike:runSpike                    # the window; -Pdevtools=<port> serves screenshots of it
./gradlew :viewport-spike:measureReadback             # per-frame readback cost at 1080p and 4K, headless
```

The readouts show the engine's and the UI's frames per second. What was measured is in the design doc's Plan tab (Phase 1 result), and the design is in its Editor tab under Viewport and play-in-editor.
