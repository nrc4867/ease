# Built-in tools

| Tool | Purpose |
|---|---|
| Scene outliner | Entity hierarchy, prefabs, layers |
| Inspector | Generated from component schemas; games override per type through `editor-api` |
| Asset browser | Import settings, GUID references, where-used search |
| Quest editor | Stage graph, aliases, objectives, actions |
| Dialogue editor | Topics, lines, responses, a live condition preview |
| Condition builder | Shared by quests, dialogue and story events |
| Render settings | Post stack, lighting and fog, tweaked live |
| Animation graph | State machines and blend trees |
| Input map | Actions, axes, default bindings, contexts |
| Debug views | Physics shapes, render graph passes, profiler, console |

Game-specific tools (an encounter table, a level generator preview) come from the game's `*-editor` module and appear next to these.
