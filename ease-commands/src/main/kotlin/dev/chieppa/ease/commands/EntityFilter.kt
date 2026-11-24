package dev.chieppa.ease.commands

import dev.chieppa.ease.ecs.replication.NetworkId

/**
 * Which entities [WorldCodec.worldToJson] writes: those whose network id is in [ids] when it is
 * given, and of those, the ones carrying a component whose serial name is [component] when it is
 * given. An entity written is written whole, every component included.
 */
public class EntityFilter(
  public val ids: Set<NetworkId>? = null,
  public val component: String? = null,
) {
  public companion object {
    /** Every entity. */
    public val ALL: EntityFilter = EntityFilter()
  }
}
