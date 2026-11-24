# Distribution

Games reach players in two stages. The bootstrap jar ships first, the way Arc does it; signed native installers follow once signing is set up. In both, the player is generic and games are separate signed packages.

| Stage | The player runs | What vouches for the code | Cost |
|---|---|---|---|
| 1. Bootstrap jar | `java -jar ease-bootstrap.jar` once; after that, a normal shortcut | The user's installed JDK is the signed executable; the bootstrap verifies everything it installs | Free; the player needs Java 25 installed |
| 2. Signed installers (planned) | A signed Windows installer, a notarized macOS `.dmg`, a Flatpak or AppImage on Linux | Windows: free certificate through SignPath Foundation. macOS: Apple Developer Program. Linux: signed checksums | Windows and Linux free; macOS 99 USD a year unless a foundation's fee waiver covers it |

## Stage 1: the bootstrap jar

- The first run installs the Ease Player (its jars and the native libraries for that platform) into a per-user folder, installs or updates the game package, and creates a launcher: a Start menu shortcut, a macOS app stub or a `.desktop` file.
- Everything it installs is checked against a manifest signed with the Ease release key, which is built into the bootstrap. Nothing unverified is installed or run.
- Later starts go straight to the installed player; updates are checked the same way.

## Stage 2: signed installers

- `jpackage` builds each platform's player from the trimmed runtime, the player and all native libraries. Games are not built into it.
- Windows builds are signed in CI through SignPath Foundation, which requires an automated, open-source build and names SignPath Foundation as the publisher. Before relying on it, confirm with them that a game runtime which loads separately signed packages is within their policy.
- macOS builds are notarized and run with the hardened runtime, plus the JIT entitlement the JVM needs.
- A file association opens game packages in the player.

## Branded export (optional)

For a developer who has their own certificates, the exporter can also build a standalone executable for one game, the way Godot exports do: the player, the runtime and that game's package bundled together under the game's name, icon and identity, then signed and notarized with the developer's own certificates. The shared Ease Player stays the free default; a branded export is the same player and package format, so nothing about the game changes.
