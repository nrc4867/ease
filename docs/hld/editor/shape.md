# Editor shape

The editor is a Stretch app that opens game projects. It compiles against the engine and `editor-api`, never against a game, so one editor serves every game.

| Module | What it is |
|---|---|
| `editor` | The app: Stretch frame, viewport, panels, undo, commands, keymap. Built on Compose Multiplatform; the engine never sees Compose. |
| `editor-api` | The plugin SPI: inspectors, panels, viewport tools and gizmos, asset editors, menu commands, importer settings UI. |
| A game's `*-editor` module | Optional. Implements `editor-api` for that game's own tools. Ships with the project, never in exports. |

A game plugs in through two entry points, both found with `ServiceLoader`:

- **`GameModule`** (from `ease-runtime`): registers components, systems, asset types, actions, conditions and render passes. The editor, the player and headless runs all call it the same way.
- **`EditorExtension`** (from `editor-api`): registers editor-only tools. Only the editor calls it.
