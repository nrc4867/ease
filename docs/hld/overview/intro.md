# Ease: Engine Architecture

Ease is the engine, named like its siblings Squash, Stretch and Arc for a principle of animation: ease in and ease out. One editor, many games. The engine is a set of libraries, the editor is an app built on them, and each game is its own project that the editor opens. Any game runs three ways: inside the editor, as an exported standalone player, or headless.

## Two projects

The engine and the first game are separate projects, and the game drives the engine's priorities.

- **Engine repo:** engine modules, editor, player, and a small sample game. Publishes versioned artifacts.
- **Game repo:** the game's modules and content, depending on a published engine version.
- **Day to day,** the game repo pulls the engine in through a Gradle composite build (`includeBuild`), so both can be edited and debugged together without publishing.
- **Priorities:** game milestones decide what the engine builds next. Engine work that no milestone needs goes on the engine's own track, and has to be proven in the sample game.
- **The sample game keeps the engine honest.** Being a second, unrelated consumer, it breaks as soon as something specific to the first game leaks into the engine.
