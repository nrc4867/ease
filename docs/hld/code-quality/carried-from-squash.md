# Carried from Squash

These Squash rules apply unchanged:

- **One way to change anything.** Every change to a world or asset is a serializable command, whether it comes from the UI, a shortcut, a test, an agent or a plugin.
- **Views never refresh each other.** Views read state and send commands; state changes recompose what reads them.
- **Derived data is a graph** that settles. Tests and agents wait for settled, never for a timer.
- **Module layers enforced by Gradle,** not by convention.
- **Text lives outside code.** User-facing strings in editor and engine UI are resource keys.
- **Original concepts only.** Engine code, tests, samples, docs and commit messages use generic or invented concepts, never a real character, franchise or brand. Game projects are separate repos with their own rules.
- **Research from papers only.** Methods are implemented from published descriptions; research code is not copied, and only permissively licensed code is ever a candidate.
- **Git workflow.** One worktree and branch per task, one squashed commit per pull request, `main` changed only through pull requests with green CI, generated files committed and checked for staleness.
