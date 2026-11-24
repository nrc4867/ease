# Terrain and deformation

Outdoor ground is a chunked heightfield; anything with overhangs (caves, arches, generated interiors) is ordinary meshes. Deformation comes in three tiers, from cheap and cosmetic to expensive and permanent, and each effect uses the cheapest tier that does the job.

## Terrain base

- Chunks of heights with level of detail by distance, streamed in around the camera.
- Layered materials blended by splat maps (grass, dirt, rock, snow), using the layered terrain material template.
- Editor tools: sculpt, smooth, flatten, paint layers, stamp from a brush texture.

## Deformation tiers

| Tier | What changes | Physics | Saved | Typical use |
|---|---|---|---|---|
| 1. Surface | A rolling deformation texture around the player, sampled by terrain and foliage shaders for displacement and normals | No | No; fades over time | Footprints, trails in snow, sand and mud; grass bending away from bodies and blasts |
| 2. Heightfield | The chunk's height data; the chunk re-meshes and its Jolt heightfield updates | Yes | Yes, as per-chunk deltas | Craters, trenches, raised walls of earth |
| 3. Volumetric | Signed-distance chunks re-meshed on the GPU (surface nets or marching cubes) | Yes, mesh colliders rebuilt | Yes, as per-chunk edits | Tunnels, collapsing overhangs, carving into cliffs |

- **Writes go through one API:** gameplay submits a brush (shape, radius, depth, falloff, tier) and the terrain system applies it on the next fixed step, so physics and visuals stay in sync.
- **Physics sync:** tier 2 and 3 edits rebuild only the touched chunks' colliders, off the main thread, and swap them in at a step boundary.
- **Saves** store edits as deltas against the authored terrain, so an unedited world costs nothing and patches to the base terrain still apply.
- **Ship order:** tier 1 first (most of the feel, little risk), tier 2 next, tier 3 only if a game needs it.
