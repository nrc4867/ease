# Engine layers

The engine is four layers of small modules. A module may use anything in a layer below it and nothing above, so the foundation can be tested alone and the renderer can be swapped without touching gameplay.

From the bottom:

1. **Core:** math, ecs, assets, jobs, serialization.
2. **Platform:** window, input, audio output, file system, clock, with desktop and headless backends.
3. **GPU and rendering:** the WebGPU wrapper and the render graph.
4. **Physics, animation and audio:** modules built on the layers below.

## In the build

`build-logic` gives every module a rank (`layers/ModuleLayers.kt`), finer than the four layers so that modules inside one layer still have an order: `ease-math`, `ease-ecs`, `ease-platform`, `ease-gpu`, `ease-render`, `ease-runtime`, then hosts such as `ease-player`, then games such as `sample`. Two checks hold the rule:

- Gradle fails the build as soon as a module declares a dependency on a module of its own rank or above.
- The Konsist test in `architecture-tests` fails when code imports a package of a module of its own rank or above.

A new module gets its rank in `ModuleLayers.kt` before it can depend on anything.

See `core.md`, `platform.md`, `gpu-and-rendering.md`, `physics-animation-audio.md` and `module-rules.md`.
