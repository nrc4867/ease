# Physics, gameplay framework and saves over the network

- **Physics:** only the authority simulates a body. Replicas are kinematic and follow snapshots. The exception is the local predicted character, which runs Jolt's character controller locally against replica colliders.
- **Terrain deformation:** edits are authority events carrying the brush, applied on the same tick everywhere. Tier 1 surface marks are cosmetic and stay local.
- **Particles and audio:** never replicated. They follow from replicated state and events, and every peer plays its own.
- **Quests, dialogue and globals:** the session's host or server holds them. Peers receive quest state and globals as replicated data and dialogue as events. Each quest declares whether it is shared by the party or per player.
- **Saves:** written by whoever holds authority over the world. Per-player data (inventory, progress) can also be saved on each player's machine and offered when joining, with the authority deciding what to trust.
