# Game packages

Both distribution stages use the same package format.

- A `.ease` archive: a manifest (id, version, required Ease version, publisher's public key), the game's jars, cooked assets and the author's ed25519 signature.
- **No native code in games.** Every native library comes with the player, which keeps macOS library validation satisfied and keeps signing to one place.
- The player verifies the signature, shows the publisher the first time a game runs, and warns clearly on an unsigned or changed package. Game code runs with the user's permissions, since the JVM has no in-process sandbox, so this check is what protects players.
- The player keeps several Ease runtime versions side by side, each from a verified release, and starts the one a game's manifest asks for.
