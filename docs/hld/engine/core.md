# Core

The core is the only layer every other module touches, so it stays small, allocation-free on hot paths, and ready for value classes.

| Module | Language | Responsibility |
|---|---|---|
| `ease-math` | Java | Vectors, quaternions, matrices, bounds, transforms. Final, immutable, never compared by identity or synchronized on, so each becomes a `value class` by adding a keyword once JDK 28's preview is usable. |
| `ease-ecs` | Kotlin | Built on Fleks: entities, components, families and systems. Systems run in fixed phases: input, simulation, physics, animation, render extract. |
| `ease-assets` | Kotlin | Every asset has a GUID. Importers (editor only) turn source files into cooked assets; loaders (runtime) read cooked assets. Both are plugins, so a game can add its own formats. |
| `ease-jobs` | Kotlin | Fixed worker pool running a per-frame job graph; no blocking on the main thread. |
| `ease-serialization` | Kotlin | Text formats for scenes and project data, so they diff cleanly in git; binary formats for cooked output. |

Simulation runs at a fixed step and rendering interpolates between steps. That keeps physics and gameplay deterministic enough for replays and headless tests.

Hot data never lives in object graphs: transforms, velocities and the like sit in ECS components or in off-heap buffers headed for the GPU. Value classes then improve the API, not the frame time.
