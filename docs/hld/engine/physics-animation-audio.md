# Physics, animation and audio

## ease-physics

`ease-physics` wraps Jolt through jolt-jni, its maintained JNI binding.

- Bodies and shapes are ECS components; a sync system moves data between the ECS and the Jolt world each fixed step.
- Collision layers, raycasts and shape queries are exposed as engine services.
- A kinematic character controller built on Jolt's virtual character handles steps, slopes and moving platforms. Games tune it; they don't rewrite it.

## ease-animation

`ease-animation` handles skeletons, clips, blending and an animation state machine edited as a graph.

- Clips and parameters can be driven by a small expression language (Molang-style), evaluated without allocation.
- Model formats arrive as importer plugins. A Blockbench/Bedrock importer ships as an optional module (`ease-import-bedrock`), since the format is general.

## ease-audio

`ease-audio` is a mixer with buses, 3D spatial sources, streaming music and snapshots (for example, muffle everything when a menu opens). The design-level behavior is in `systems/audio.md`.
