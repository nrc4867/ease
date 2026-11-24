# Decisions

| Topic | Decision | Why |
|---|---|---|
| Name | Ease | An animation principle, like Squash, Stretch and Arc |
| Physics bindings | jolt-jni | Better maintained than writing and keeping our own |
| ECS | Fleks | Kotlin-first and maintained; the engine adds phases and replication on top |
| Editor viewport | Engine and Stretch share one wgpu device; Stretch reads each frame back for Compose | No public way to hand a wgpu texture to Compose on the GPU; the readback holds 60 fps (see `editor/viewport-and-play.md`) |
| Windows and gamepads | SDL3 through LWJGL 3.4 | One native library for windows, their native handles (the Metal layer on macOS) and gamepads with rumble, gyro and a broad controller database |
| Lighting path | Clustered forward+ first; deferred optional behind the same pipeline interface | Custom shading and transparency favor forward+; materials never know the path |
| Networking | Network-ready from day one; single-player runs as a local host plus a local client over loopback | Authority and replication bugs surface immediately instead of being patched in later |
| P2P signaling | Deferred; client-server and direct connect first | The object model matters at the start, the matchmaking path does not |
| In-game UI | Own engine UI, drawn by the renderer | Controller-first focus navigation and one look across HUD, menus and dialogue |
| Target hardware | A Steam Deck class floor machine at 60 fps and 1280×800, with the budgets in `targets.md` | One concrete low-end machine turns performance into numbers every feature is held to |
| Hot-path math | Immutable `ease-math` types for the API, plus in-place forms (`…Into(dst)`, operations on `float[]`) for hot paths | Allocation budgets then hold by construction instead of depending on the JIT removing temporary objects |
| Command serialization | kotlinx.serialization: JSON for agents, scripts and plugins; a compact binary form for the command log and the network; schemas for `describe` written at build time | One declaration serves every reader of a command; replicated components keep their own KSP serializers for quantized deltas |
| Agent tools | The official MCP Kotlin SDK over streamable HTTP on 127.0.0.1, in a host-layer module | The same stack Arc runs; both libraries pass the license rule |
| Versioning | Semantic `0.x` releases, exact pins in game projects, an integer `formatVersion` with step-by-step code migrations on every surface outside the repo (see `versioning.md`) | Game repos upgrade on purpose, and old files and saves keep loading without golden fixtures |
