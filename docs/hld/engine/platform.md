# Platform

`ease-platform` is a set of interfaces (window, input devices, audio output, file system, clock) with two backends. Nothing above this layer imports LWJGL; the architecture tests check it.

- **`platform-desktop`** uses SDL3 through LWJGL for windows, their native handles (including the Metal layer on macOS) and gamepads (rumble, gyro, broad controller database), and miniaudio, through our own FFM bindings, for audio output.
- **`platform-headless`** has no window, scripted or recorded input, and a null audio device. It is what tests, CI and servers run on.

## Input

Input is controller-first through an action layer:

- Games define **actions** (`Dodge`, `Confirm`) and **axes** (`Move`, `Look`), never raw buttons.
- Bindings map actions to any device; players can rebind, and bindings are saved per player.
- **Input contexts** stack (gameplay, menu, dialogue), so opening a menu swaps what buttons mean without touching game code.
- The active device is tracked, so UI shows the right button glyphs and switches instantly between keyboard and controller.

## Windows and threads

`Windowing` opens windows and drains their events; each `Window` hands the GPU layer a `SurfaceHandle` (Win32, Metal layer, Xlib or Wayland) and its size in physical pixels. All of it runs on the process's first thread, which macOS requires for windows; desktop apps start with `-XstartOnFirstThread` there (`desktopApp` in `build-logic`).

Programs that open real windows are smoke apps in a module's `smoke` source set, run by Gradle tasks such as `windowSmoke`, never by `check`. CI runs them on all three desktops, under a virtual X server on Linux.
