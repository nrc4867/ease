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
