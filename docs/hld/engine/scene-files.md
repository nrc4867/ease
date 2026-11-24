# Scene files

Scenes and prefabs are files people edit on branches and merge. A merge conflict is acceptable when a person can read and resolve it; the format is shaped so that unrelated edits never conflict.

- **One YAML file per scene or prefab.** YAML block lists carry no separators, so appending a node never changes the line above it. Large levels split into sublevels, each its own file, so people mostly work in different files.
- **Random local ids.** Every node has a random 64-bit id, unique within its file. Two branches that each add a node never pick the same id, and nodes sorted by id scatter new entries through the file instead of piling them at the end.
- **Links on the child.** A node stores its parent and an order key that can always be split (a node fits between `a` and `b` as `an`). No node lists its children, so adding a child changes only the new node.
- **Canonical output.** Keys are sorted, floats use the shortest form that reads back exactly, strings are quoted unless plainly safe, and nothing holds a timestamp. Saving an unchanged scene writes identical bytes.
- **No editor state.** Camera position, selection and expanded rows live in a git-ignored per-user file, so opening a scene never changes it.
- **Overrides** on a prefab instance are a path (local id, component type, field) plus a value, sorted by path, one per entry.
- **Sublevel ids:** a node's key at runtime is the chain of placement ids down to its local id, so two copies of one sublevel never collide.

## Assets

- Each source file has a `.meta` file beside it holding its GUID. Moving a file in the editor moves both; a meta file found without its source is matched to a new file by content hash.
- Binary assets cannot be merged. Projects mark them `lockable` for Git LFS, and a person locks one before editing it.
- An asset check in CI fails on a meta file without a source, a source without a meta file, a GUID used twice or a reference that does not resolve. The asset database applies the same rules when it loads.
