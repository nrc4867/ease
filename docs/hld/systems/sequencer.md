# Sequencer

The sequencer is the engine's timeline for anything authored over time: cutscenes in games, trailers, and stills and animations in tools built on the engine. A still is just a one-frame sequence with photo render settings.

A **sequence** asset holds tracks bound to entities through named bindings, the same idea as quest aliases. The same sequence can then play against different characters or places.

| Track | Drives |
|---|---|
| Transform | Position, rotation, scale, keyed with curves |
| Animation | Clips and blends on a skeleton, layered over its state machine |
| Camera | Cuts, focal length, depth-of-field focus, camera shake |
| Particles | Play, stop and effect parameters |
| Audio | Sound events, music cues, bus snapshots |
| Material | Any material instance parameter |
| Physics | Spans where bodies simulate live, or a baked result |
| Events | Gameplay actions: set a quest stage, speak a dialogue line, trigger a story event |

**Physics and particles when scrubbing.** Simulated systems can't jump to an arbitrary time. In the editor they re-simulate from the nearest cached frame; for shots that must look exactly the same every time, physics spans bake to transform tracks and particle effects use fixed seeds.

**Three ways to play:**

1. **In game:** cutscenes run on the normal loop, can be skipped, and hand the camera back to gameplay with a blend.
2. **In the editor:** scrubbable preview, edited on Stretch's timeline and curve editor.
3. **Offline render:** headless offscreen mode at a fixed frame rate, with supersampling and sub-frame motion blur, writing image sequences. Video is encoded by an external encoder from those frames.
