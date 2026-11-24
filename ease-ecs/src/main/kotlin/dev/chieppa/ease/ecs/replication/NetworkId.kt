package dev.chieppa.ease.ecs.replication

import dev.chieppa.ease.annotations.Doc
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Doc(
  "An entity's id for the whole session. It is never reused, so commands, the command log and " +
    "the network name entities by it. Ids run from 1 up to, not including, 2^53, " +
    "so a script holds every id exactly as a number."
)
@Serializable
@SerialName("ease.NetworkId")
@JvmInline
public value class NetworkId(@Doc("The id as a number.") public val value: Long) {
  /** Whether an entity can carry this id: from 1 up to, not including, [LIMIT]. */
  public val isValid: Boolean
    get() = value in 1 until LIMIT

  override fun toString(): String = "#$value"

  public companion object {
    /** The first id no entity may carry, 2^53: the ids a JavaScript number holds exactly. */
    public const val LIMIT: Long = 1L shl 53
  }
}
