# In-game UI

Games draw menus, HUD and dialogue with the engine's own UI module, rendered by the same renderer as the world. Compose stays in the editor and tools.

- **Retained and data-driven:** UI screens are assets (a tree of panels, text, images, lists, bars) styled by theme data, so artists and agents edit them as files and the editor previews them live.
- **Controller first:** every screen has focus navigation by direction, with explicit overrides where layout guesses wrong; mouse and touch work on top, never the other way round. Input contexts switch automatically when a screen opens.
- **Text:** FreeType and HarfBuzz for shaping, signed-distance glyphs (msdfgen) for crisp text at any size, localization keys for every string, and per-character reveal for dialogue boxes.
- **Bound to game state:** widgets bind to ECS components, quest state and globals, and redraw when those change; they send commands, never edit state directly.
- **In the world as well as on screen:** the same UI can render into a texture on a world surface (a sign, a terminal) or follow an entity (a name tag, a health bar).
- **Animated through the sequencer and material parameters,** so menu transitions use the same tools as cutscenes.
