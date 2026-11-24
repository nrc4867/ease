# Distance fields

`ease-sdf` is an optional module. The world stays meshes and heightfields; distance fields are a tool that produces them, plus an aid to rendering.

| Use | When it runs | Output |
|---|---|---|
| Procedural organic spaces | Load time | Shapes combined with smooth CSG, meshed once on the GPU, then static meshes with normal colliders |
| Tier 3 deformation | Runtime, opt-in per area | Edited chunks re-meshed, colliders swapped at a step boundary |
| Editor blockout | Editor time | CSG shapes for greyboxing, baked to meshes when done |
| Scene distance field | Every frame, coarse | Soft shadows, ambient occlusion, particle collision, volumetric shapes |

Meshes made from distance fields have no authored UVs, so they use triplanar materials. The layered terrain template supports that mode.
