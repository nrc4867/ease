package dev.chieppa.ease.ecs.replication

import com.github.quillraven.fleks.Entity

/**
 * Which entities a state packet names that a [StatePacketReader] passes over, reading their state
 * without applying it, rather than applying it or refusing the packet: such as an entity a client
 * removed after the packet was sent. Asked for every entity of every packet, so it allocates
 * nothing.
 */
public interface SkippedEntities {
  /** Whether state for the entity [id], carried here by [entity] or by none, is passed over. */
  public fun skips(id: NetworkId, entity: Entity): Boolean

  public companion object {
    /** Passes over no entity. */
    public val NONE: SkippedEntities =
      object : SkippedEntities {
        override fun skips(id: NetworkId, entity: Entity): Boolean = false
      }
  }
}
