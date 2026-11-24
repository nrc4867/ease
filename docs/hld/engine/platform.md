# Platform

`ease-platform` is a set of interfaces (window, input devices, audio output, file system, clock) with two backends. Nothing above this layer imports LWJGL.

- **`platform-desktop`** uses LWJGL: GLFW for windows and native handles, SDL for gamepads (rumble, gyro, broad controller database), OpenAL Soft for audio output.
- **`platform-headless`** has no window, scripted or recorded input, and a null audio device. It is what tests, CI and servers run on.

## Input

Input is controller-first through an action layer:

- Games define **actions** (`Dodge`, `Confirm`) and **axes** (`Move`, `Look`), never raw buttons.
- Bindings map actions to any device; players can rebind, and bindings are saved per player.
- **Input contexts** stack (gameplay, menu, dialogue), so opening a menu swaps what buttons mean without touching game code.
- The active device is tracked, so UI shows the right button glyphs and switches instantly between keyboard and controller.
