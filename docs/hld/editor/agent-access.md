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

## In the code

`ease-agent` (layer Host) is the headless host: one game's world, frozen at its tick, served over MCP by the official MCP Kotlin SDK on Ktor's CIO engine, at `http://127.0.0.1:<port>/mcp` only. Every request needs `Authorization: Bearer <token>`, a Host naming the loopback and that port, and no `Origin`; anything else gets 401 or 403.

It offers five tools; each is an `AgentTool` listed in `AgentTools`, so a new tool is one new class.

| Tool | Does here |
|---|---|
| `describe` | Every component and command type the world's `WorldCodec` knows, each with a schema read from its generated `SerialDescriptor`: serial name, kind, fields and their types, nested types spelled out |
| `query` | The current tick and the entities as JSON, every field written; narrowed by entity ids or a component type |
| `capture` | A PNG of the world through the game's `camera()`, optionally moved by `centerX`, `centerY` and `visibleHeight`, at the requested size (640x360 by default, 1920x1080 at most). The GPU opens on the first capture; with no adapter it answers an error |
| `logs` | Lines the host recorded (server start, the game loading, checks run, rejected commands, tool errors) from a marker, with the next marker |
| `test` | `replay` rebuilds the world from the game and its command log at the logged ticks and compares the JSON, listing each differing value; `ticks` runs a scratch copy N more ticks and fails on a throw |

The live world is only read. `test` works on scratch worlds made by `WorldReplica`, which a play world can reuse. All world and GPU work runs on one thread: `AgentWorld.access` moves each call onto a single-threaded dispatcher the host passes in.

Run the sample game's host with `./gradlew :sample:runAgentHost` (`--args='--port <n>'`; the default port is 47840). The token comes from `EASE_AGENT_TOKEN`, or is generated and printed once with the line that connects Claude Code:

```
claude mcp add --transport http ease http://127.0.0.1:47840/mcp --header "Authorization: Bearer <token>"
```
