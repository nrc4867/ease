# Materials

A material is a shader template plus a parameter block. Templates are written by programmers; artists and game code only make instances.

- **Templates** are WGSL files that implement a surface function ("given this pixel, return albedo, normal, roughness, emissive...") and declare their parameters and textures. The engine wraps them in the lighting, shadow and depth passes, so one template works everywhere. Shipped templates: lit (PBR metal/roughness), unlit, toon ramp, foliage (wind, two-sided), water, layered terrain.
- **Instances** pick a template and override values. Instances can derive from other instances, so a variant changes only what differs.
- **Permutations** come from feature flags (normal map, alpha clip, skinning, vertex color). They compile on first use, get cached on disk, and are warmed during cooking so players never see a hitch.
- **Bind groups** follow one convention everywhere: 0 = frame, 1 = view or pass, 2 = material, 3 = object. Core WebGPU has no bindless textures, so draws are batched by material and small textures go into arrays or atlases.
- **Textures** carry their own sampler settings, including nearest filtering for pixel-art textures and per-texture mip and anisotropy choices.
- **Animated parameters:** any material parameter (dissolve, hit flash, emissive pulse) can be driven by the animation system or by gameplay code through the instance.

In the editor, parameters are edited live with Stretch's color picker and curve editor. Templates hot-reload on save. A node-based shader graph is a later option that would generate the same template files.
