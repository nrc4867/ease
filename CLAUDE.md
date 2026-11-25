# Ease

A game engine and its editor: Kotlin by default, Java for the hot core (`ease-math`), WebGPU through wgpu-native over FFM. The editor is a Stretch app; Stretch is included as a composite build from the sibling `stretch` folder. Games are separate repos that include Ease the same way.

## Read before working

| What | Where |
| --- | --- |
| The design and the plan (source of truth) | Ease: Engine Architecture: https://claude.ai/artifact/SYVVq4ZBz5Khy5XfYTwNpk (read the step's row, then every tab on its phase's Design line) |
| An offline mirror of the design, regenerated from the doc; may lag, never edit by hand | `docs/hld/` |
| The code rules and how each is enforced | Code quality tab (mirrored in `docs/hld/code-quality/`) |

Before starting engine work, name the plan phase and the game need it serves. When the doc and `docs/hld/` differ, the doc wins. If building shows the design is wrong or missing something, change the doc's tab first (or comment on it when the change needs a decision), then the code. After a step, record what was decided in the phase's Decisions or Result notes and set the step's Status.

## Build and test

Gradle needs JDK 25 as `JAVA_HOME`. Stretch is found as `../stretch` or through `stretch.dir` in `~/.gradle/gradle.properties`. wgpu-native's bindings come from GitHub Packages, so `~/.gradle/gradle.properties` also needs `GitHubPackagesUsername` and `GitHubPackagesPassword` (a token with `read:packages`).

```
./gradlew check                  # everything CI's check job runs
./gradlew spotlessApply          # format Kotlin (ktfmt, Google style) and Java (google-java-format)
./gradlew checkSources           # the rules no linter covers
./gradlew :ease-ecs:test         # one module's tests
./gradlew :ease-ecs:jmh          # JMH benchmarks (JSON under build/results/jmh); never part of check
./gradlew gpuTest                # the *GpuTest tests; need a WebGPU adapter
./gradlew generateNotice         # rewrite NOTICE after a dependency change; check fails while it is stale
./gradlew installGitHook         # the pre-commit hook; any compile installs it too
./gradlew :ease-cli:installDist  # the ease command, under ease-cli/build/install/ease/bin
./gradlew :ease-cli:runEase --args="new empty --name 'My Game' --dir ../my-game"   # ease new, from Gradle
./gradlew :ease-cli:runEase --args="check --dir ../my-game"   # ease check on a game (exit 1 on an error; --fix repairs)
tools/check-templates.sh         # generate a game from every template, ease check it, build it, run its tests; CI runs it
```

While working, run only the tests for what changed (`./gradlew :<module>:test --tests '<TestClass>'`). CI runs the rest on the pull request.

Native libraries (wgpu-native from webgpu-native-ffm, SDL3 from LWJGL) resolve only this machine's natives jar. `-Pnatives=<platform>` (`windows-x64`, `linux-x64`, `linux-arm64`, `macos-x64`, `macos-arm64`) picks another, for packaging. A library module takes the bindings with `implementation` and the natives only for its tests or smoke apps; hosts such as `ease-player` take the natives with `runtimeOnly` (see `NativePlatform`, `WebgpuNatives` and `LwjglNatives` in `build-logic`).

Programs that open real windows are smoke apps, never tests: `./gradlew :ease-platform:windowSmoke` opens, resizes and closes a window; `:sample:sampleSmoke` runs the sample game in the player for 120 frames and resizes halfway. `:sample:runSample` runs it until its window is closed. `:sample:sampleHeadless` runs it in the player with no window or GPU for 600 ticks (`--headless <ticks>`), host and local client as in every run; CI's check job runs it. `:sample:assembleEaseTypes` joins every module's TypeScript declarations into the sample's `ease.d.ts`. `:sample:runAgentHost` serves the sample world to agents over MCP until stopped and prints the `claude mcp add` line (see `docs/hld/editor/agent-access.md`); it is not part of `check`. CI runs the smoke apps; run one locally only when changing what it covers. The player runs whichever one `GameModule` is on its classpath, so a game, not the player, owns the run tasks: it takes `ease-player` with `runtimeOnly`.

## Modules

A module depends only on modules in lower layers. Gradle fails a dependency that breaks this, and the Konsist test in `architecture-tests` fails an import that does. A new module needs its layer in `build-logic/.../layers/ModuleLayers.kt` and its `include` in `settings.gradle.kts`.

| Layer | Module | Holds |
| --- | --- | --- |
| Game | `sample` | The sample game that keeps the engine free of any one game |
| Host | `ease-player` | The generic launcher every game runs in; finds the game's `GameModule` on the classpath |
| Host | `ease-agent` | The headless agent host: a game's world served to agents over MCP on 127.0.0.1 (`describe`, `query`, `capture`, `logs`, `test`) |
| Host | `ease-cli` | The `ease` command line: `new` (a game from a project template, put in git unless `--no-git`), `generate` (a module, component or system), `templates`, `check` (`--fix` for the safe repairs, `--staged` for what git's index holds) and `merge-driver` (run by git); the templates are folders in its resources, with hooks for what runs after `new` |
| Host | `viewport-spike` | The Phase 1 editor viewport check (engine on Stretch's device); never ships |
| Runtime | `ease-runtime` | Hosted worlds, ticking, game modules, and the local host: a host world plus a local client world over the loopback transport, which the player always runs |
| Render | `ease-render` | The extract snapshot, 2D cameras and the quad renderer |
| GPU | `ease-gpu` | wgpu device, surfaces, pipelines |
| Net | `ease-net` | Sessions between peers: the channel interface, the `Transport` interface and the loopback transport with its tick-counted network simulator, the join handshake (protocol version, types hash, content digest), relevancy filters and each peer's view, spawns and despawns by asset GUID and node key, delta snapshots in priority order with owner-only fields filtered, their acknowledgements, per-tick input with redundancy and a tick window, and the authority's command checks and rate limits |
| Project | `ease-project` | A whole project as git holds it: `ProjectChecker` (sidecars, GUIDs, references, scene structure, conflict markers, Git LFS coverage), `ProjectFixer`, the merge driver, and the git setup `ease new` writes (`.gitignore`, `.gitattributes`, LFS, the driver, the pre-commit hook) |
| Scene | `ease-scene` | Nodes (`NodeKey`, `Parent`, slots), scenes and prefabs as folders of canonical YAML (one file per node), prefab instances with overrides, sublevels, and scene reloads as commands |
| Assets | `ease-assets` | Asset GUIDs and `.meta` sidecars, the asset database (folder assets included), loaders and reload commands, and the file association service |
| Platform | `ease-platform` | Windows, input, audio output, file watching, clocks |
| Data | `ease-serialization` | Canonical YAML (sorted keys, shortest floats, stable bytes) and format-version migrations, for meta files and later scenes |
| World | `ease-commands` | Commands with their scope, targets and `validate`, the permission check, undo and redo for user origins, the command log, their JSON and compact binary formats |
| Core | `ease-ecs` | Entities, components, systems, phases; network ids, authority, `@Replicated`, the bit codec, replicated serializers and state packets |
| Foundation | `ease-annotations` | `@Doc`, `@Range` and `@SerializedAs`: the documentation, ranges and serializer hints that `ease-codegen` turns into `ease.d.ts` and JSON Schemas; every public serialized type and field carries `@Doc` |
| Foundation | `ease-math` | Vectors, quaternions, matrices, bounds, transforms (Java) |
| Test support | `ease-testing` | The allocation harness (`AllocationBudget`); test configurations only (Java) |
| Build tool | `ease-codegen` | The KSP processors: `@Replicated` components' serializers, and for every `@Serializable` type a JSON Schema and a TypeScript declaration under `META-INF/ease`; `ksp` configurations only, never shipped |

`build-logic` holds the convention plugins (`ease.kotlin-module`, `ease.java-module`, `ease.root`, `ease.licenses`, and for games `ease.game-settings`, `ease.game-root`, `ease.game-module`, and `ease.game-app` for the module holding the game's `GameModule`), `checkSources` and its rules, and the license check, each with a test.

## Rules the checks enforce

- Formatting: ktfmt Google style and google-java-format, 100 columns. `spotlessApply` fixes it.
- detekt (`config/detekt/detekt.yml`): line length, chained calls, nesting, method and class size, complexity, no `@Deprecated`. Remove an old API and change every caller instead.
- `checkSources`: code files warn over 300 lines and fail over 400; Markdown fails over 150; no sleeps, delays, `waitUntil`, timed awaits or polling in any code; no reflection or `@VisibleForTesting` in tests; no history words in comments; no golden files or a flag that rewrites them.
- Tests check public behavior: inputs and outcomes. Async code takes its dispatcher and clock from its constructor; tests advance them under `runTest`, or advance a world by ticks.
- Comments and Markdown describe the code as it stands. No changelogs, dates or session notes; git history is the history.
- Engine code, tests, samples, docs and commit messages use invented concepts, never a real character, franchise or brand.

## Git

One branch and worktree per task, a pull request per task, squashed on merge. `main` takes only pull requests whose `CI` check passes.
