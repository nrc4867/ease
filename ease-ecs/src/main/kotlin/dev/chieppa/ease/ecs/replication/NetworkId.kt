package dev.chieppa.ease.ecs.replication

import dev.chieppa.ease.annotations.Doc
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Doc(
  "An entity's id for the whole session. It is never reused, so commands, the command log and " +
    "the network name entities by it. Ids run from 1 up to, not including, 2^53, " +
    "so a script holds every id exactly as a number. The host issues the ids below 2^52, which " +
    "are the only ones on the wire; every other peer issues its own entities' ids from 2^52 up."
)
@Serializable
@SerialName("ease.NetworkId")
@JvmInline
public value class NetworkId(@Doc("The id as a number.") public val value: Long) {
  /** Whether an entity can carry this id: from 1 up to, not including, [LIMIT]. */
  public val isValid: Boolean
    get() = value in 1 until LIMIT

  override fun toString(): String = "#$value"

  /** Whether this id lies in the host's range, the only ids peers name to each other. */
  public val isShared: Boolean
    get() = value in 1 until LOCAL_START

  public companion object {
    /** The first id no entity may carry, 2^53: the ids a JavaScript number holds exactly. */
    public const val LIMIT: Long = 1L shl 53

    /**
     * The first id of the range a peer other than the host issues for its own entities, 2^52. Those
     * entities never go on the wire, so their ids never meet the host's.
     */
    public const val LOCAL_START: Long = 1L shl 52
  }
}
