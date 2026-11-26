# Ease

A game engine and its editor: Kotlin by default, Java for the hot core (`ease-math`), WebGPU through wgpu-native over FFM. The editor is a Stretch app; Stretch is included as a composite build from the sibling `stretch` folder. Games are separate repos that include Ease the same way.

## Read before working

| What | Where |
| --- | --- |
| The design and the plan (source of truth) | Ease: Engine Architecture: https://claude.ai/artifact/SYVVq4ZBz5Khy5XfYTwNpk (read the step's row, then every tab on its phase's Design line) |
| The code rules and how each is enforced | The doc's Code quality and setup tab |
| Module layers, ranks and the core types | The doc's Engine tab |
| Agent tools and what the editor shows | The doc's Editor tab |

Before starting engine work, name the plan phase and the game need it serves. If building shows the design is wrong or missing something, change the doc's tab first (or comment on it when the change needs a decision), then the code. After a step, record what was decided in the phase's Decisions or Result notes and set the step's Status.

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

Native libraries (wgpu-native from webgpu-native-ffm, SDL3 from LWJGL, Jolt from jolt-jni) resolve only this machine's natives jar. `-Pnatives=<platform>` (`windows-x64`, `linux-x64`, `linux-arm64`, `macos-x64`, `macos-arm64`) picks another, for packaging. A library module takes the bindings with `implementation` and the natives only for its tests or smoke apps; hosts such as `ease-player` take the natives with `runtimeOnly` (see `NativePlatform`, `WebgpuNatives`, `LwjglNatives` and `JoltNatives` in `build-logic`).

Programs that open real windows are smoke apps, never tests: `./gradlew :ease-platform:windowSmoke` opens, resizes and closes a window; `:sample:sampleSmoke` runs the sample game in the player for 120 frames and resizes halfway, then prints its frame times (the window opens at 1280x800; `--size <w>x<h>` picks another). `:sample:runSample` runs it until its window is closed. `:sample:sampleHeadless` runs it in the player with no window or GPU for 600 ticks (`--headless <ticks>`), host and local client as in every run; CI's check job runs it. The sample starts from its saved scene in `sample/content` (`playfield.scene`, written with `SceneFolder`, never by hand); its run tasks pass that folder to the player and the agent host as `--content <folder>`, and CI's check job runs `ease check` on it (`./gradlew :ease-cli:runEase --args="check --dir sample"`). `:sample:assembleEaseTypes` joins every module's TypeScript declarations into the sample's `ease.d.ts`. `:sample:runAgentHost` serves the sample world to agents over MCP until stopped and prints the `claude mcp add` line (see the Editor tab's Agent access section); it is not part of `check`. CI runs the smoke apps; run one locally only when changing what it covers. The player runs whichever one `GameModule` is on its classpath, so a game, not the player, owns the run tasks: it takes `ease-player` with `runtimeOnly`.

## Modules

A module depends only on modules in lower layers. Gradle fails a dependency that breaks this, and the Konsist test in `architecture-tests` fails an import that does. A new module needs its layer in `build-logic/.../layers/ModuleLayers.kt` and its `include` in `settings.gradle.kts`.

| Layer | Module | Holds |
| --- | --- | --- |
| Game | `sample` | The sample game that keeps the engine free of any one game |
| Host | `ease-player` | The generic launcher every game runs in; finds the game's `GameModule` on the classpath |
| Host | `ease-agent` | The headless agent host: a game's world served to agents over MCP on 127.0.0.1 (`describe`, `query`, `capture`, `logs`, `test`) |
| Host | `ease-cli` | The `ease` command line: `new` (a game from a project template, put in git unless `--no-git`), `generate` (a module, component or system), `templates`, `check` (`--fix` for the safe repairs, `--staged` for what git's index holds) and `merge-driver` (run by git); the templates are folders in its resources, with hooks for what runs after `new` |
| Host | `viewport-spike` | The Phase 1 editor viewport check (engine on Stretch's device); never ships |
| Runtime | `ease-runtime` | Hosted worlds, ticking, game modules (shared, predicted and host systems, scene types and the start scene), a game's content folder (`GameContent`) with its scenes placed and reloaded on the host, and the local host: a host world plus a local client world over the loopback transport, which the player always runs; the client world predicts its own entities, reconciles and replays, and interpolates the rest |
| Import | `ease-importers` | The engine's importers, found through the service loader like a game's: the glTF importer (meshes, materials, textures, skins, clips and a prefab of the node tree, each a sub-asset) |
| Render | `ease-sdf-gpu` | Distance fields on the GPU and in physics: `ease-sdf`'s programs as generated WGSL (`SdfShader`, `@GpuStruct` node parameters), surface nets in compute passes into a `ComputedMesh` the lit pass draws, and `SdfMesh.collider()` for static `MeshCollider`s |
| Render | `ease-render` | The scene's render components (`Mesh` with the built-in shapes, `MeshRef` for a mesh asset, `Material`, `MaterialRef` for a material asset, `Camera`, `DirectionalLight`, `CustomMesh` for meshes built at runtime, `SurfaceTextures` for a mesh's textures), the render asset formats and loaders (`.mesh`, `.material`), the extract snapshot at world transforms (skinned meshes with their joint matrices), the world renderer, `TextureUpload` (cooked textures to the GPU, decoding block compression where the device lacks it) and `MaterialTextureCache` (a material asset's textures as a texture set) |
| GPU | `ease-gpu` | wgpu device, surfaces, pipelines, the frame arena a frame's descriptors and uploads go through, mesh data (the dynamic mesh API and meshes computed on the device), compute kernels and buffers, sampled textures (`GpuTexture`, samplers), and the lit forward pass with its depth buffer, explicit bind group layout, material templates, texture sets, `@GpuStruct` uniforms and skinned draws (`SkinVertices`, joint matrices in a storage buffer) |
| Net | `ease-net` | Sessions between peers: the channel interface, the `Transport` interface and the loopback transport with its tick-counted network simulator, the join handshake (protocol version, types hash, content digest), relevancy filters and each peer's view, spawns and despawns by asset GUID and node key, delta snapshots in priority order with owner-only fields filtered, their acknowledgements, per-tick input with redundancy and a tick window, each peer's input for systems, the client's clock sync from the host's input-slack reports, and the authority's command checks and rate limits |
| Project | `ease-project` | A whole project as git holds it: `ProjectChecker` (sidecars, GUIDs, references, scene structure, conflict markers, Git LFS coverage), `ProjectFixer`, the merge driver, and the git setup `ease new` writes (`.gitignore`, `.gitattributes`, LFS, the driver, the pre-commit hook) |
| Animation | `ease-animation` | Skeletal animation: the `.skin`, `.clip` (with events) and `.animator` formats and loaders, `Animator`, `SkinRef` and `SocketFollower`, clip sampling, 1D blends, the state machine with crossfades, poses given to `WorldTransforms` (never written to joints' components), sockets, clip events, the probes they run against physics, and skinned meshes' joint matrices (`SkinPalettes`) |
| Physics | `ease-physics` | Jolt through jolt-jni: colliders (shapes and static triangle meshes) and rigid bodies as components kept in step each fixed step, game-named collision layers, surface types, raycasts and surface queries, trigger volumes and sensors, the character controller behind `Mover`, and the physics events scripts will subscribe to |
| Scene | `ease-scene` | Nodes (`NodeKey`, `Parent`, slots), `WorldTransforms` (each node placed by its parents, derived every tick), scenes and prefabs as folders of canonical YAML (one file per node), prefab instances with overrides, sublevels, and scene reloads as commands |
| Media | `ease-textures` | Textures as assets: the cooked form (KTX2 with BC7, BC5, RGBA8 or RGBA16F levels), the texture importer (images decoded through `ease-platform`, box-filtered mipmaps, our own BC7 and BC5 encoders, bytes identical on every OS; `cookTexture` for images inside other files) and the `.ktx2` loader |
| Assets | `ease-assets` | Asset GUIDs and `.meta` sidecars, the asset database (folder assets included), importers and their cache (sibling files read through `ImportContext` are part of the key), loaders of sources and of cooked sub-assets, reload commands, and the file association service |
| Platform | `ease-platform` | Windows, input, audio output, file watching, clocks, image decoding (stb_image and tinyexr) and the texture sizes the cooked form and the GPU share |
| Data | `ease-sdf` | Distance fields on the CPU: shapes built from code (sphere, box, rounded box, capsule, cylinder, plane, torus), transforms and smooth CSG compiled into one program, its Kotlin evaluation, and surface nets meshing whose output is bit-identical on every machine; depends only on `ease-math` |
| Data | `ease-serialization` | Canonical YAML (sorted keys, shortest floats, stable bytes) and format-version migrations, for meta files and later scenes |
| Data | `ease-gltf` | Our own glTF 2.0 reader: `.gltf` with buffers beside it or in data URIs, and `.glb`; accessors of every type (normalized, sparse), images, `KHR_lights_punctual`; errors name the element |
| World | `ease-commands` | Commands with their scope, targets and `validate`, the permission check, undo and redo for user origins, the command log, their JSON and compact binary formats |
| Core | `ease-ecs` | Entities, components, systems, phases, predicted systems; network ids, authority, `@Replicated`, the bit codec, replicated serializers and state packets; the stores behind prediction (history, rewind, correction offsets) and interpolation; the `Mover` interface and its kinematic mover |
| Foundation | `ease-annotations` | `@Doc`, `@Range` and `@SerializedAs`: the documentation, ranges and serializer hints that `ease-codegen` turns into `ease.d.ts` and JSON Schemas; every public serialized type and field carries `@Doc` |
| Foundation | `ease-memory` | Off-heap memory: `FrameArena` (a native block per frame in flight, handed out in slices; stale slices throw in debug builds), `NativeWriter` cursors, `MappedFile` little-endian views, and `@GpuStruct` |
| Foundation | `ease-math` | Vectors, quaternions, matrices, bounds, transforms (Java); trigonometry through `StrictMath`, so every JVM computes the same bits |
| Test support | `ease-testing` | The allocation harness (`AllocationBudget`); test configurations only (Java) |
| Build tool | `ease-codegen` | The KSP processors: `@Replicated` components' serializers, each `@GpuStruct` class's writer and WGSL struct, and for every `@Serializable` type a JSON Schema and a TypeScript declaration under `META-INF/ease`; `ksp` configurations only, never shipped |

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
