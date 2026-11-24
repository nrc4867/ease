# Ease

A game engine and its editor: Kotlin by default, Java for the hot core (`ease-math`), WebGPU through wgpu-native over FFM. The editor is a Stretch app; Stretch is included as a composite build from the sibling `stretch` folder. Games are separate repos that include Ease the same way.

## Read before working

| What | Where |
| --- | --- |
| The design, one small file per topic | `docs/hld/` (index in `docs/hld/README.md`) |
| The code rules and how each is enforced | `docs/hld/code-quality/` |
| The plan: phases and their gates | [Engine Architecture, Plan tab](https://claude.ai/artifact/SYVVq4ZBz5Khy5XfYTwNpk) |

Before starting engine work, name the plan phase and the game need it serves.

## Build and test

Gradle needs JDK 25 as `JAVA_HOME`. Stretch is found as `../stretch` or through `stretch.dir` in `~/.gradle/gradle.properties`. wgpu-native's bindings come from GitHub Packages, so `~/.gradle/gradle.properties` also needs `GitHubPackagesUsername` and `GitHubPackagesPassword` (a token with `read:packages`).

```
./gradlew check                  # everything CI's check job runs
./gradlew spotlessApply          # format Kotlin (ktfmt, Google style) and Java (google-java-format)
./gradlew checkSources           # the rules no linter covers
./gradlew :ease-ecs:test         # one module's tests
./gradlew gpuTest                # the *GpuTest tests; need a WebGPU adapter
./gradlew generateNotice         # rewrite NOTICE after a dependency change; check fails while it is stale
./gradlew installGitHook         # the pre-commit hook; any compile installs it too
```

While working, run only the tests for what changed (`./gradlew :<module>:test --tests '<TestClass>'`). CI runs the rest on the pull request.

Native libraries (wgpu-native from webgpu-native-ffm, SDL3 from LWJGL) resolve only this machine's natives jar. `-Pnatives=<platform>` (`windows-x64`, `linux-x64`, `linux-arm64`, `macos-x64`, `macos-arm64`) picks another, for packaging. A library module takes the bindings with `implementation` and the natives only for its tests or smoke apps; hosts such as `ease-player` take the natives with `runtimeOnly` (see `NativePlatform`, `WebgpuNatives` and `LwjglNatives` in `build-logic`).

Programs that open real windows are smoke apps, never tests: `./gradlew :ease-platform:windowSmoke` opens, resizes and closes a window; `:ease-player:playerSmoke` draws 120 frames of the triangle and resizes halfway. `:ease-player:runPlayer` runs the player until its window is closed. CI runs them; run one locally only when changing what it covers.

## Modules

A module depends only on modules in lower layers. Gradle fails a dependency that breaks this, and the Konsist test in `architecture-tests` fails an import that does. A new module needs its layer in `build-logic/.../layers/ModuleLayers.kt` and its `include` in `settings.gradle.kts`.

| Layer | Module | Holds |
| --- | --- | --- |
| Game | `sample` | The sample game that keeps the engine free of any one game |
| Host | `ease-player` | The generic launcher every game runs in |
| Host | `viewport-spike` | The Phase 1 editor viewport check (engine on Stretch's device); never ships |
| Runtime | `ease-runtime` | Worlds, ticking, views, game modules |
| Render | `ease-render` | The render graph |
| GPU | `ease-gpu` | wgpu device, surfaces, pipelines |
| Platform | `ease-platform` | Windows, input, audio output, files, clocks |
| Core | `ease-ecs` | Entities, components, systems, phases |
| Foundation | `ease-math` | Vectors, quaternions, matrices, bounds, transforms (Java) |
| Test support | `ease-testing` | The allocation harness (`AllocationBudget`); test configurations only (Java) |

`build-logic` holds the convention plugins (`ease.kotlin-module`, `ease.java-module`, `ease.root`, `ease.licenses`), `checkSources` and its rules, and the license check, each with a test.

## Rules the checks enforce

- Formatting: ktfmt Google style and google-java-format, 100 columns. `spotlessApply` fixes it.
- detekt (`config/detekt/detekt.yml`): line length, chained calls, nesting, method and class size, complexity, no `@Deprecated`. Remove an old API and change every caller instead.
- `checkSources`: code files warn over 300 lines and fail over 400; Markdown fails over 150; no sleeps, delays, `waitUntil`, timed awaits or polling in any code; no reflection or `@VisibleForTesting` in tests; no history words in comments; no golden files or a flag that rewrites them.
- Tests check public behavior: inputs and outcomes. Async code takes its dispatcher and clock from its constructor; tests advance them under `runTest`, or advance a world by ticks.
- Comments and Markdown describe the code as it stands. No changelogs, dates or session notes; git history is the history.
- Engine code, tests, samples, docs and commit messages use invented concepts, never a real character, franchise or brand.

## Git

One branch and worktree per task, a pull request per task, squashed on merge. `main` takes only pull requests whose `CI` check passes.
