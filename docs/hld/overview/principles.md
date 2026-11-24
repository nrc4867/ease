# Principles

- **The engine knows no game.** No game names, types or content live in engine modules. Anything two different games could both use belongs to the engine; anything only one game needs belongs to that game.
- **The editor never compiles against a game.** It depends on the engine and on `editor-api`, and loads a game project at runtime.
- **A game never depends on the editor.** Editor-only code for a game (custom inspectors, tools) lives in a separate `*-editor` module that ships with the project but not in exports.
- **One code path everywhere.** Editor play mode, the standalone player and headless runs boot the same game the same way.
- **WebGPU is the only GPU API.** Rendering goes through wgpu-native via FFM, which targets Vulkan, Metal and D3D12. LWJGL covers windows and input; audio is miniaudio through our own FFM bindings.
- **Kotlin by default, Java for the hot core.** Math is written in Java so its types can become value classes when Valhalla ships, with no change to Kotlin callers.
- **Stretch supplies the editor shell.** The engine is built as its own code base.
