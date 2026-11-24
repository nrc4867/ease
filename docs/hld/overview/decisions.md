# Decisions

| Topic | Decision | Why |
|---|---|---|
| Name | Ease | An animation principle, like Squash, Stretch and Arc |
| Physics bindings | jolt-jni | Better maintained than writing and keeping our own |
| ECS | Fleks | Kotlin-first and maintained; the engine adds phases and replication on top |
| Gamepads | SDL3 through LWJGL 3.4 | Rumble, gyro and a broad controller database |
| Lighting path | Clustered forward+ first; deferred optional behind the same pipeline interface | Custom shading and transparency favor forward+; materials never know the path |
| Networking | Network-ready from day one; single-player runs as a local host plus a local client over loopback | Authority and replication bugs surface immediately instead of being patched in later |
| P2P signaling | Deferred; client-server and direct connect first | The object model matters at the start, the matchmaking path does not |
| In-game UI | Own engine UI, drawn by the renderer | Controller-first focus navigation and one look across HUD, menus and dialogue |
