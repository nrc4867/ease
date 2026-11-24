# Particles and VFX

Particles simulate on the GPU in compute shaders. They are visual only: gameplay never reads them, so they can run at frame rate rather than the fixed simulation step.

An **effect** asset holds one or more emitters, and each emitter is a stack of modules:

| Stage | Modules |
|---|---|
| Spawn | Rate, bursts, spawn on distance travelled, spawn from a mesh surface or a skeleton bone |
| Initialize | Shape (point, sphere, cone, box, mesh), velocity, lifetime, size, rotation, color |
| Update | Gravity and drag, curl noise, attractors, color, size and alpha over life (curves), collision against the depth buffer |
| Events | Sub-emitters on spawn, death or collision |
| Render | Billboards, stretched billboards, ribbons and trails, meshes; flipbook textures; soft depth fade; simple lighting; a distortion (heat haze) pass |

- **Sorting:** transparent emitters sort on the GPU; additive ones skip it.
- **Budgets:** each effect has a particle cap and a cull distance; the engine has a global cap and lowers spawn rates past it.
- **Triggering:** gameplay plays an effect with parameters (position, attachment, color, scale, intensity) and gets a handle back to stop or move it.
- **Determinism:** effects take a seed, so replays and headless captures look the same.

The effect editor previews live in the viewport and uses Stretch's curve editor and timeline for over-life curves and bursts.
