package dev.chieppa.ease.net.command

import dev.chieppa.ease.ecs.replication.PeerId

/** Told of every command the authority refused, with the peer, the layer and the reason. */
public fun interface CommandRefusals {
  /** [peer]'s command was refused at [layer] for [reason]; it changed nothing. */
  public fun refused(peer: PeerId, layer: CommandLayer, reason: String)

  public companion object {
    /** Keeps no record. */
    public val IGNORE: CommandRefusals = CommandRefusals { _, _, _ -> }
  }
}
