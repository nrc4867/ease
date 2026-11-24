# Dependency rules

The module dependency graph has six parts and two runtime links:

- The **engine** modules (layered; see `engine/layers.md`) sit at the bottom.
- **`editor-api`** depends on the engine.
- The **editor** depends on the engine and on `editor-api`.
- The **player** depends on the engine.
- A **game** depends on the engine.
- A game's **`*-editor` module** depends on `editor-api` and the game.
- The two runtime links, drawn dashed, are the editor loading a game and the player loading a game.

Every solid arrow points down toward the engine, and nothing points back up. The editor and the player reach a game only by loading it at runtime, which is what lets one editor host many games. A build check enforces it: any engine module that imports from `editor` or a game fails the build.
