# Runtime and hosts

An exported game is the generic player, the game's own jars and its cooked content. Nothing from the editor ships.

The engine never owns `main()`. Every program that uses it is a **host**: it creates worlds, decides when they tick, and decides where their cameras render. The player, the editor and Squash are three hosts on the same engine; none is a special case.

`ease-runtime` gives hosts these building blocks:

- **Worlds:** create, load a scene into, and dispose of any number of independent worlds.
- **Ticking:** step a world one fixed tick on demand, or run it on the standard loop. A world nobody steps stays frozen.
- **Views:** render any camera in any world into a window surface or an offscreen texture.
- **Game modules:** attach a project's `GameModule` to a world, and detach it again for hot reload.
- **Services:** assets, audio, input and networking are shared by the host's worlds, not owned by any one of them.

| Host | Worlds | Ticking | Output |
|---|---|---|---|
| Player | A host world and a local client world, even in single-player | Standard loop | A window, or nothing when headless |
| Editor | An edit world that never ticks, play worlds, server and client worlds for network tests, preview worlds for thumbnails | Only play worlds tick | Offscreen textures shown in Stretch viewports |
| Squash | A stage world per scene | On demand while scrubbing; fixed rate for offline renders | Offscreen textures and image sequences |

The editor is therefore not a game built on the engine: its UI is Compose, its main world never simulates, and it has privileged access to importers, the asset database and `editor-api` that no game sees. Keeping the host concept explicit stops the player's or the editor's assumptions from leaking into the engine.

## In the code

- **`GameModule`** (`dev.chieppa.ease.runtime`): a game's entry point. `registerTypes` adds its command and component types to the world's codec, `systems` adds its phased systems, `populate` creates its starting entities through commands (`Spawn`), so the history and the command log hold them, and `camera` gives the host a starting `Camera2D`. A game lists its module in `META-INF/services/dev.chieppa.ease.runtime.GameModule`; `GameModules.load()` returns the one on the classpath and fails, naming what it found, when there is none or several.
- **`HostedWorld`**: one world built from a `GameModule` and a `FixedStep`. It holds the Fleks world (the module's systems plus the engine's `ExtractSystem` in `Phase.EXTRACT`), the `WorldCodec`, the `CommandHistory`, the `RenderSnapshot` and the current tick. `step()` runs one tick with the fixed step as the delta; `execute(command, origin)` goes through the history stamped with the current tick.
- **`WorldRunner`**: runs a `HostedWorld` on `FixedStepLoop` from an injected `Clock`. A host calls `frame()` once per frame and renders with `alpha`.
- **The snapshot** (`dev.chieppa.ease.render`): `ExtractSystem` copies every entity with a `LocalTransform` and a `Quad` into the `RenderSnapshot` each tick, moving last tick's position to previous; an entity seen for the first time starts with previous equal to current. Its arrays are indexed by entity id and grow only past their capacity, so a tick of a steady world allocates nothing.
- **Quads:** `WorldRenderer.render(snapshot, alpha, camera, target, encoder)` places each quad between its previous and current position by `alpha` and draws them all with `InstancedQuadPass` from `ease-gpu`: one instanced draw, six vertices a quad, each quad read from a storage buffer by instance index. `Camera2D` gives the center and the visible height; the width follows the target's aspect ratio.

The player (`ease-player`) loads the one game on its classpath, runs it on a `WorldRunner` with `SystemClock`, and draws it into its window every frame. A headless host is a `HostedWorld` stepped by hand, or a `WorldRunner` on a `ManualClock`; an offscreen host renders into an `OffscreenTarget`. The sample game's tests run it headless and offscreen, the same world the player runs with `./gradlew :sample:runSample`.
