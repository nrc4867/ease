# Module rules

The test for any engine change: would a second, unrelated game want it unchanged? If not, it belongs in a game.

- No game names, species, moves, items or story data in engine code, tests or sample assets. Engine samples use neutral placeholders.
- No genre rules. The engine has no health stat, no turn order and no damage formula; it offers the hooks those are built from.
- Extension happens through registration (components, systems, importers, render passes, condition functions), never through `if (game == ...)`.
- Each module publishes a small public API; internals are `internal` in Kotlin or package-private in Java.
- A build check fails any engine module that imports from the editor or from a game.
