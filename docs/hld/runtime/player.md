# Player

`ease-player` is one generic launcher used by every game:

1. Read the cooked content's manifest.
2. Start a platform backend: desktop, or headless when asked.
3. Load the game jar and call its `GameModule`, exactly as the editor's play mode does.
4. Load the start scene and run the fixed-step loop.

The player has no game-specific code; branding (name, icon, splash) comes from the project file.
