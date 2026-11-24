# Headless

The player runs without a window in three modes, all on the same code as a normal run.

| Mode | Window | GPU | Used for |
|---|---|---|---|
| Offscreen | No | Yes | Render tests that assert measured properties, thumbnails, captures |
| Simulation | No | No | Gameplay, quest and save tests in CI; bots; a future server |
| Replay | Optional | Optional | Re-running recorded input to reproduce a bug exactly |

Replay works because simulation is fixed-step and input goes through the action layer, so a recording is just a list of actions per tick. A headless run exits with a non-zero code when a test fails, so CI can gate on it.
