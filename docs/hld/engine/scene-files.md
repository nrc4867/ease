# Scene files

Scenes and prefabs are files people edit on branches and merge. The format is shaped so that edits to different nodes never conflict in plain git; only two edits to the same node can, and a person can read and resolve those.

- **A folder per scene or prefab, one file per node.** `cave.scene/` holds `scene.yaml` (format version and scene settings) and `nodes/<id>.yaml`, one per node. Editing, adding, removing or reordering a node changes only that node's file. Large levels split into sublevels, each its own folder. Cooking packs each folder into one file for the player.
- **Random local ids.** Every node has a random 64-bit id, unique within its scene, which is also its file name. Two branches that each add a node never pick the same id.
- **Links on the child.** A node stores its parent and an order key that can always be split (a node fits between `a` and `b` as `an`). No node lists its children, so adding or moving a child changes only that child's file.
- **Canonical output.** Keys are sorted, floats use the shortest form that reads back exactly, strings are quoted unless plainly safe, and nothing holds a timestamp. Saving an unchanged scene writes identical bytes, and saving after one edit rewrites only the files that changed.
- **No editor state.** Camera position, selection and expanded rows live in a git-ignored per-user file, so opening a scene never changes it.
- **Overrides** on a prefab instance are a path (local id, component type, field) plus a value, sorted by path, in the instance node's file.
- **Sublevel ids:** a node's key at runtime is the chain of placement ids down to its local id, so two copies of one sublevel never collide.

## Assets

- Each source file has a `.meta` file beside it holding its GUID. Moving a file in the editor moves both; a meta file found without its source is matched to a new file by content hash.
- Binary assets cannot be merged. Projects mark them `lockable` for Git LFS, and a person locks one before editing it.
- `ease check`, in the pre-commit hook and CI, fails on a meta file without a source, a source without a meta file, a GUID used twice a reference that does not resolve, or a node whose parent is missing. The asset database applies the same rules when it loads.
