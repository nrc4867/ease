package dev.chieppa.ease.ecs.replication

import com.github.quillraven.fleks.Entity

private const val INITIAL_ENTITIES = 64

/**
 * Which entities the last state packet a world applied carried, by entity id, so prediction can
 * tell an entity the newest snapshot brought from one it left as an older snapshot had it. Nothing
 * here allocates once it has grown to the world's entity ids.
 */
internal class CarriedEntities {
  private var packet = 0L
  private var stamps = LongArray(INITIAL_ENTITIES)

  /** A packet is about to apply: entities [mark]ed from now on are its own. */
  fun startPacket() {
    packet++
  }

  /** The packet being applied carries state for [entity]. */
  fun mark(entity: Entity) {
    val id = entity.id
    if (id >= stamps.size) stamps = stamps.copyOf(maxOf(id + 1, stamps.size * 2))
    stamps[id] = packet
  }

  /** Whether the last packet applied carried state for [entity]. */
  fun inLastPacket(entity: Entity): Boolean =
    packet > 0 && entity.id < stamps.size && stamps[entity.id] == packet
}
