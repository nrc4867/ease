# Testing

**Test behavior through public APIs.** A test sets up inputs, calls public API and checks outcomes: returned values, resulting world state, emitted events. It does not check which internal methods ran or in what order. When internals deserve their own tests, that is a sign they are a separate responsibility: extract them into their own class with its own public API, and test that.

- No reflection to widen visibility, no `setAccessible`, no `@VisibleForTesting` workarounds.
- Fakes over mocks: small working implementations of interfaces (an in-memory file system, a loopback transport, a scripted input device) rather than mock frameworks verifying calls.

**No waits.** No `Thread.sleep`, `delay`, `waitUntil`, timed `await` or polling loop, in tests or engine code. Async code takes its dispatcher and clock from its constructor; tests pass `StandardTestDispatcher` and a test clock and advance them under `runTest`. The engine's fixed step helps: a test advances a world by ticks, never by wall time.

**No goldens.** No committed expected-output files, reference images or `updateGoldens` flags. Tests state the property that matters instead:

- Geometry: mesh invariants (closed, no degenerate triangles, normals outward), bounds, volume within tolerance.
- Rendering: measured facts about the frame (an object covers the expected screen region, no NaN or black frame, a lit side is brighter than its shadow side, pixel probes at chosen points).
- Serialization: round trips (write, read, compare) and migrations from small fixtures written in code.

**Tiers,** lowest that can catch the bug:

| Tier | What | Where it runs |
|---|---|---|
| Unit | Plain Kotlin and Java, no window, no GPU; milliseconds per test | Locally and CI |
| Scenario | Commands applied to a world through the real command system, then outcomes checked | Locally and CI |
| Integration | Whole systems together: physics, networking over loopback, save and load | CI, sharded |
| GPU | Real wgpu on software Vulkan; render facts and compute shaders | CI on Linux |
