# Plugins

The editor UI, agents and plugins all reach the engine through one surface: commands, schemas and registered contributions. Plugins come in two kinds, split by trust.

| Kind | Runs | Trust | Reaches the engine through |
|---|---|---|---|
| Game modules and editor extensions | In process, Kotlin, compiled | Full | The engine API, `GameModule`, `editor-api` |
| Tool plugins | A separate host, TypeScript | Limited to declared capabilities | `describe`, `query`, `apply`, `capture`, plus panels, panes and actions |
| Agents (for comparison) | MCP | Limited, with confirmations | The same tools as tool plugins |

**Tool plugins** follow the design proven in Arc:

- A manifest names the plugin's id, version, API version and **capabilities** (panels, actions, which commands it may apply, files, network hosts). The user sees them before any plugin code runs, every call is checked against them, and a version that asks for more stays off until re-approved.
- Plugins run in a separate JS host with React for their UI, so a broken plugin cannot take the editor down.
- Types and the API reference are generated from the engine's Kotlin definitions, so they never drift from the real API.

**Boundaries:**

- Anything that runs inside a shipped game is a JVM module with full trust. The sandboxed host is for editor tooling only.
- Untrusted code always runs out of process. The JVM has no in-process sandbox (the Security Manager is gone), so mods for shipped games, if ever wanted, would use the same separate-host approach.

**Shared with other apps:** the plugin host (manifest, capability checks, JS host, type generation) has no domain logic, so it belongs in Stretch as a module that Arc, Squash and this editor share. Each app supplies only its capability list and its API.
