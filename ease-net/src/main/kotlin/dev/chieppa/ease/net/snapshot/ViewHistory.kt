package dev.chieppa.ease.net.snapshot

import dev.chieppa.ease.ecs.replication.SkippedEntities

/**
 * The spawns and despawns a client has applied, numbered from 1, as its snapshots read them: a
 * snapshot applies once the client has applied the latest spawn it needs, and passes over an entity
 * spawned again or despawned after that one.
 */
internal interface ViewHistory : SkippedEntities {
  /** How many spawns and despawns were applied. */
  val applied: Long

  /** Starts reading a snapshot whose latest spawn is number [needs]. */
  fun reading(needs: Long)

  /**
   * A snapshot whose latest spawn is number [needs] applied. Every later snapshot was built after
   * it, so a despawn up to [needs] is never asked about again.
   */
  fun appliedThrough(needs: Long)
}
