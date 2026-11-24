package dev.chieppa.ease.ecs.replication

import kotlinx.serialization.Serializable

/**
 * An entity's id for the whole session. Unlike a Fleks entity id it is never reused, so commands,
 * the command log and the network name entities by it. Ids start at 1.
 */
@Serializable
@JvmInline
public value class NetworkId(public val value: Long) {
  override fun toString(): String = "#$value"
}
