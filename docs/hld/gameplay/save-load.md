# Save and load

A save is a snapshot of persistent state only; everything else is rebuilt from content on load.

- **Saved by the framework:** quest states, current stages, alias fills, globals, said-once dialogue, story manager history.
- **Saved by opt-in:** ECS components marked persistent, and any game data a game registers a serializer for.
- **Format:** versioned binary for shipping, a JSON debug form for inspection. Each game declares migrations from older save versions.
- **Slots and autosave** are engine features; when to autosave is the game's call.

Headless tests load every save fixture after each content change, so broken saves show up in CI rather than in a player's hands.
