# CI on GitHub's free runners

Everything runs on GitHub's standard hosted runners, with no self-hosted machines and no GPU.

- **Main pipeline on `ubuntu-latest`:** formatting and source checks, unit and scenario tests, integration tests in parallel shards, allocation budgets.
- **GPU tests on software Vulkan:** the job installs Mesa's lavapipe driver and wgpu runs on it. Slow, but real, and no GPU needed.
- **Other platforms:** a smaller Windows and macOS job builds, loads the natives (wgpu, Jolt, SDL, networking) and runs unit tests, so platform breaks surface without tripling the full suite.
- **Smoke apps:** programs that open real windows run in CI, on Linux under a virtual X server (`xvfb-run`): `windowSmoke` on all three desktops, and `:sample:sampleSmoke`, which runs the sample game in the player for 120 frames and resizes the window halfway, on Linux (lavapipe) and macOS, since Windows runners have no GPU adapter. They are never part of `check`, so local test runs open no windows.
- **Speed:** Gradle's setup action with build caching, the configuration cache, and a changed-files step that skips code jobs when a pull request touches only Markdown, as Squash does.
- **Benchmarks** run on pushes to `main` and nightly, not on every pull request.
- `main` accepts only pull requests whose required jobs pass, merged as one squashed commit.
