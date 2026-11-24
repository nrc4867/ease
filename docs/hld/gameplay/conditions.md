# Conditions, globals and scripting hooks

Conditions are the shared vocabulary of quests, dialogue and story events. Each one is a registered function, a comparison and a value, joined with AND/OR, so the editor can build them from dropdowns.

| Kind | Examples | Registered by |
|---|---|---|
| Quest state | stage of quest X >= 30; quest Y running | Engine |
| World | entity has tag; distance between aliases; current location; time of day | Engine |
| Globals | global `chapter` = 3 | Engine |
| Dialogue | line already said | Engine |
| Game rules | anything specific to one game | The game |

**Globals** are typed, named variables (int, float, bool, string) that are saved with the game.

**Scripting** is Kotlin. A game registers new condition functions and action types in code; content references them by id. There is no embedded scripting language: Kotlin hot reload in the editor covers iteration speed, and one can be added behind the same registry.
