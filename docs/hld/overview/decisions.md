# Decisions

| Topic | Decision | Why |
|---|---|---|
| Name | Ease | An animation principle, like Squash, Stretch and Arc |
| Physics bindings | jolt-jni | Better maintained than writing and keeping our own |
| ECS | Fleks | Kotlin-first and maintained; the engine adds phases and replication on top |
| Windows and gamepads | SDL3 through LWJGL 3.4 | One native library for windows, their native handles (the Metal layer on macOS) and gamepads with rumble, gyro and a broad controller database |
| Lighting path | Clustered forward+ first; deferred optional behind the same pipeline interface | Custom shading and transparency favor forward+; materials never know the path |
| Networking | Network-ready from day one; single-player runs as a local host plus a local client over loopback | Authority and replication bugs surface immediately instead of being patched in later |
| P2P signaling | Deferred; client-server and direct connect first | The object model matters at the start, the matchmaking path does not |
| In-game UI | Own engine UI, drawn by the renderer | Controller-first focus navigation and one look across HUD, menus and dialogue |
| Target hardware | A Steam Deck class floor machine at 60 fps and 1280×800, with the budgets in `targets.md` | One concrete low-end machine turns performance into numbers every feature is held to |
| Versioning | Semantic `0.x` releases, exact pins in game projects, an integer `formatVersion` with step-by-step code migrations on every surface outside the repo (see `versioning.md`) | Game repos upgrade on purpose, and old files and saves keep loading without golden fixtures |
