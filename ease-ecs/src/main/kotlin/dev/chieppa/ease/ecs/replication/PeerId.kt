package dev.chieppa.ease.ecs.replication

import kotlinx.serialization.Serializable

/** One peer of a session: the host, a server or a client. */
@Serializable
@JvmInline
public value class PeerId(public val value: Int) {
  override fun toString(): String = "peer $value"

  public companion object {
    /** The peer that runs the session; a single-player world is its host. */
    public val HOST: PeerId = PeerId(0)
  }
}
