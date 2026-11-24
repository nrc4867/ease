# Commands

Commands are the only way anything outside the simulation changes a world: the editor, agents, scripts, plugins and the network all send them. `ease-commands` holds them, one rank above `ease-ecs`.

- **A command** is `@Serializable` data implementing `Command.apply(world)`, which changes the world and returns the command that undoes it exactly. A command that cannot apply throws `CommandRejected` and changes nothing.
- **Built-ins:** `SetTransform` (an entity's `LocalTransform`), `Spawn` (an entity with given components), `Destroy`, and `Restore` (an entity back under its own id with exactly its components, which is how a destroy is undone).
- **Registration:** `worldCodec { command(...); component(...) }` adds a module's or game's own command and component types to the built-ins.

## Formats

`WorldCodec` reads and writes commands from the same declarations in two forms:

- **JSON** for agents, scripts and plugins, the type under a `type` key (`"type":"ease.SetTransform"`).
- **Binary** (CBOR) for the command log and the network.

`worldToJson` writes every entity with all its components in entity order; tests compare whole worlds with it. Replicated components use their own generated serializers (see `networking/replication.md`).

## History and log

`CommandHistory` applies commands to one world with undo and redo, each change stamped with its tick and `CommandOrigin` (user, agent, script, network, game). Commands and inverses are kept as bytes and read fresh before each apply, so a command never shares objects with the world it changed.

`CommandLog` holds every applied command in order, undo and redo included, as bytes. Replaying it onto a world in the state its first entry saw reproduces every change.
