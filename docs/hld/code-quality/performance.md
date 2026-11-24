# Performance

Performance is a top concern, so it is measured continuously, in the form that shared CI machines can measure reliably.

- **Allocation budgets gate CI.** Hot paths (ECS iteration, the frame loop over a benchmark scene, math, serialization, replication) have tests that count allocations per frame or per operation. Allocation counts are stable on any machine, so these fail a build when they regress.
- **Timing is tracked, not gated.** JMH benchmarks for hot paths and a headless benchmark scene run in CI and record results as artifacts. Shared runners are too noisy to fail builds on time; trends are reviewed and big regressions investigated.
- **Profiling** uses JFR for the CPU and RenderDoc for the GPU, both reachable from the editor.
- **Design rules** that keep it fast: hot data in ECS components or off-heap buffers; no allocation in per-frame code; no reflection or `kotlin-reflect` at runtime; prefer primitive arrays to boxed collections on hot paths.
