# Viewport and play-in-editor

The editor and the engine share one wgpu device: the one Stretch's `GpuSurface` owns. Each frame the engine records its passes with its own bindings onto the surface's encoder and into its output view (`GpuContext.nativeHandles` and `GpuFrame.nativeHandles` in Stretch, `GpuContext.shared`, `CommandEncoder.borrowed` and `RenderTarget.borrowed` in `ease-gpu`), so nothing is copied between engine and editor. Stretch then reads the frame back into an image Compose draws, about a frame behind, so Compose draws rulers, selection and gizmos on top with no native child window.

The readback is the one copy per frame. wgpu-native exposes no Direct3D 12 or Vulkan texture handles and Compose's drawing context is not public, so a texture cannot reach Compose on the GPU. Measured with `:viewport-spike:measureReadback` on an Intel Arc 140V, a frame waiting for its own readback costs 3.8 ms at 1080p and 29 ms at 4K; Stretch overlaps two frames, so the viewport holds 60 fps at about 2560 by 1600 while the editor process uses about half a core. Play standalone shows the game without the readback.

- **Edit mode:** the edit world is never simulated. Only editor tools change it, through undoable commands.
- **Play mode:** pressing Play copies the edit world into a separate play world and runs the normal runtime loop on it, with viewport input routed to the platform layer. Stop throws the play world away.
- **Play standalone:** launches the real player as its own process on the current project, to catch anything that only breaks outside the editor.
