# 3D audio

Audio runs on its own thread over OpenAL Soft, which brings positional sound, HRTF for headphones and EFX reverb and filters. The engine adds everything a game designer touches.

- **Sound events** are assets, not files: a container of clips with random pick, pitch and volume variation, and per-event attenuation. Gameplay plays an event, never a raw clip.
- **Emitters and the listener** are ECS components. In first person the listener sits on the camera, so sound follows the player's head.
- **Attenuation** uses curves per event (volume, low-pass, spread over distance); doppler is optional per event.
- **Occlusion:** a few Jolt raycasts per audible emitter, spread across frames, set a low-pass and volume drop when walls are in the way.
- **Reverb zones** are volumes with presets (cave, hall, open field) that blend as the listener moves between them.
- **Buses:** master, music, effects, voice, ambience, UI. Snapshots duck or filter buses together (dialogue ducks music; a menu muffles the world).
- **Voice limiting:** a cap on real voices; the quietest or lowest-priority sounds become virtual and resume when they matter again.
- **Music:** streamed, with layered stems that fade in and out by intensity, and crossfades between tracks on beat boundaries.
- **Haptics:** controller rumble is driven like audio, through events on its own bus via SDL3, so an explosion's sound and rumble come from one trigger.

If OpenAL's mixing becomes limiting, the plan B is FMOD behind the same event API; game code wouldn't change.
