package dev.chieppa.ease.ecs.replication

import dev.chieppa.ease.annotations.Doc
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Doc("One peer of a session: the host, a server or a client. The host is 0.")
@Serializable
@SerialName("ease.PeerId")
@JvmInline
public value class PeerId(@Doc("The peer as a number.") public val value: Int) {
  override fun toString(): String = "peer $value"

  public companion object {
    /** The peer that runs the session; a single-player world is its host. */
    public val HOST: PeerId = PeerId(0)
  }
}
