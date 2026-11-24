# Squash as a consumer

Squash, the toon character tool, uses the engine as a library for its planned Stills and Animation Studio stages. It is a third consumer alongside the first game and the sample game, and like them it is never a dependency of the engine.

- **What Squash uses:** scene, render graph, physics, particles and the sequencer, on the same wgpu device as its Stretch UI. It does not use the engine's editor.
- **What Squash gives back:** its cel-shaded look (flat and two-tone shading, outlines, surface-drawn fur and face marks), packaged as an optional engine material template plus post passes. Games and Squash then render characters the same way.
- **Assets:** Squash characters and objects move into the engine as native assets. glTF export is only for leaving the ecosystem.
- **Ordering:** Squash's physics and particle shots wait on the engine's sequencer. A simple poses-and-camera stills stage can ship in Squash before that.
