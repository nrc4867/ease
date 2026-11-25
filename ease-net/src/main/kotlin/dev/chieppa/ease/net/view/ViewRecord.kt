package dev.chieppa.ease.net.view

import com.github.quillraven.fleks.Entity
import dev.chieppa.ease.ecs.replication.NetworkId
import dev.chieppa.ease.net.snapshot.ViewHistory

private const val INITIAL_ENTRIES = 64

/**
 * What a client's spawns and despawns did, numbered from 1 in the order they applied: the number of
 * the spawn behind each live replica, and the ids of replicas despawned since the snapshots applied
 * last needed. A snapshot whose latest spawn is number `n` was built before any later spawn or
 * despawn, so it passes over a replica spawned by a later one, whose spawn holds newer state, and
 * over an id a later one despawned; any other id it names must be live. Reading allocates nothing;
 * a despawn allocates only while the list grows.
 */
internal class ViewRecord : ViewHistory {
  private var spawnedBy = LongArray(INITIAL_ENTRIES)
  private var goneIds = LongArray(INITIAL_ENTRIES)
  private var goneBy = LongArray(INITIAL_ENTRIES)
  private var goneCount = 0
  private var needs = 0L

  override var applied: Long = 0L
    private set

  /** Records that the next message spawned [entity]. */
  fun spawned(entity: Entity) {
    applied++
    if (entity.id >= spawnedBy.size) {
      spawnedBy = spawnedBy.copyOf(maxOf(entity.id + 1, spawnedBy.size * 2))
    }
    spawnedBy[entity.id] = applied
  }

  /** Records that the next message despawned the replica [id], which [entity] carried. */
  fun despawned(id: NetworkId, entity: Entity) {
    applied++
    if (entity.id < spawnedBy.size) spawnedBy[entity.id] = 0L
    if (goneCount == goneIds.size) {
      goneIds = goneIds.copyOf(goneCount * 2)
      goneBy = goneBy.copyOf(goneCount * 2)
    }
    goneIds[goneCount] = id.value
    goneBy[goneCount] = applied
    goneCount++
  }

  override fun reading(needs: Long) {
    this.needs = needs
  }

  override fun appliedThrough(needs: Long) {
    var kept = 0
    for (index in 0 until goneCount) {
      if (goneBy[index] > needs) {
        goneIds[kept] = goneIds[index]
        goneBy[kept] = goneBy[index]
        kept++
      }
    }
    goneCount = kept
  }

  override fun skips(id: NetworkId, entity: Entity): Boolean {
    if (entity != Entity.NONE) return entity.id < spawnedBy.size && spawnedBy[entity.id] > needs
    var skipped = false
    for (index in 0 until goneCount) {
      if (goneIds[index] == id.value && goneBy[index] > needs) skipped = true
    }
    return skipped
  }
}
