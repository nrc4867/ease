package dev.chieppa.ease.ecs.replication

import kotlinx.serialization.Serializable

/**
 * An entity's id for the whole session. Unlike a Fleks entity id it is never reused, so commands,
 * the command log and the network name entities by it. Ids start at 1 and stay below [LIMIT], so an
 * id from a peer can never push the next issued id past `Long.MAX_VALUE`.
 */
@Serializable
@JvmInline
public value class NetworkId(public val value: Long) {
  /** Whether an entity can carry this id: from 1 up to, not including, [LIMIT]. */
  public val isValid: Boolean
    get() = value in 1 until LIMIT

  override fun toString(): String = "#$value"

  public companion object {
    /** The first id no entity may carry, 2^62. */
    public const val LIMIT: Long = 1L shl 62
  }
}
