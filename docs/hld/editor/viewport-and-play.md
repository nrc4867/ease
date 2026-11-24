# Viewport and play-in-editor

The editor and the engine share one wgpu device. The engine renders the scene into an offscreen texture and Stretch's viewport composites it, so Compose can still draw rulers, selection and gizmos on top. This is the main payoff of using WebGPU on both sides: no native child window, no readback.

- **Edit mode:** the edit world is never simulated. Only editor tools change it, through undoable commands.
- **Play mode:** pressing Play copies the edit world into a separate play world and runs the normal runtime loop on it, with viewport input routed to the platform layer. Stop throws the play world away.
- **Play standalone:** launches the real player as its own process on the current project, to catch anything that only breaks outside the editor.
