# GPU and rendering

Rendering is split in two: a thin GPU layer that wraps WebGPU, and a renderer built as a render graph on top of it.

## ease-gpu

`ease-gpu` wraps wgpu-native through FFM bindings, which run on Vulkan, Metal and D3D12.

- It shares one bindings module, `webgpu-native-ffm`, with Stretch's `stretch-gpu`, so the editor process loads exactly one copy of wgpu.
- Shaders are WGSL and hot-reload in the editor.
- It renders to a window surface (from the platform's native handle) or to an offscreen texture (editor viewport, headless captures).

**Bindings are our own.** They are generated from `webgpu.h`, pinned to one wgpu-native release, and live in a standalone `webgpu-native-ffm` module (https://github.com/nrc4867/webgpu-native-ffm). Stretch and Ease both depend on it. No third-party WebGPU wrapper.

## ease-render

`ease-render` is a render graph. Passes declare what they read and write; the graph orders them, allocates transient textures, and drops passes nothing reads. The default pipeline:

1. Depth and normal prepass, plus motion vectors
2. Shadows: cascades for the sun, an atlas for local lights
3. Clustered forward+ lighting in HDR
4. Screen-space passes: ambient occlusion (GTAO), optional reflections
5. Volumetric fog and light shafts (froxel grid)
6. Post stack: TAA, bloom, depth of field, tonemapping (AgX), LUT color grading

The post stack is data, not code: an ordered list of passes with parameters, which the editor can tweak live. A game adds a stylized look by registering its own passes through the same graph API, never by forking the renderer.

A dynamic mesh API builds and uploads meshes at runtime, for generated levels and debug geometry.

## Lighting pipelines

**Lighting pipelines are swappable.** Clustered forward+ ships first. The lighting path is a render-graph pipeline chosen per project or per view, and every pipeline produces the same outputs for the post stack. Material templates only implement a surface function, so they work with any pipeline. A deferred pipeline can be added behind the same interface; materials with a custom shading model (toon, cel) would draw in its forward pass, which it needs for transparency anyway.
