# Agent access

Agents work through files first and a small MCP server second. The MCP tool set is fixed; new engine features become reachable through schemas and commands, never through new tools.

**Files are the primary interface.** Scenes, prefabs, materials, quests, dialogue and sequences are diffable text with generated schemas. An agent edits them like code and the editor hot-reloads them, so every feature with a file format is agent-ready at no extra cost. A typed Kotlin builder for content, in the spirit of Squash's kit, can be added.

**MCP covers what files can't:** seeing, running and inspecting live state.

| Tool | Does |
|---|---|
| `describe` | Schemas for component types, asset types, commands and actions, generated at build time. How the agent discovers features. |
| `query` | Read entities, components and assets by selector; JSON back |
| `apply` | A batch of editor commands as one undoable transaction; returns what changed |
| `play` | Start, step N ticks, pause or stop a play world; inject input actions |
| `capture` | Render a camera or viewport to an image, or snapshot the editor window |
| `logs` | Console, errors, warnings and profiler output since a marker |
| `test` | Run headless tests or replays; pass/fail with failures |
| `run` | Escape hatch: a short Kotlin script against the engine API in a scratch world |

**The rule:** features add commands, components and asset types, never tools. A new terrain brush is a command that `describe` lists and `apply` runs; a new particle module is a component in a schema.

**Where it runs:**

- **In the editor,** listening on 127.0.0.1 only. Agent changes go through the same command and undo system as the user's, appear live in the viewport, and can be undone.
- **In a headless host,** kept warm, for background and CI work. Same tools, no window.

**Guardrails:** agent actions are labeled in the undo history; deletes and anything outside the project folder need the user's confirmation; `run` scripts get a scratch world unless the user allows more.

Squash can expose the same tools from its recipe server, so one agent workflow covers both projects.
