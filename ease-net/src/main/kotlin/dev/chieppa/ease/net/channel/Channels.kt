package dev.chieppa.ease.net.channel

import dev.chieppa.ease.commands.WorldCodec
import dev.chieppa.ease.ecs.replication.StatePackets

/** The message sizes and close codes every [Connection] carries. */
public object Channels {
  /** The longest unreliable message: one datagram on every common path. */
  public const val MAX_UNRELIABLE_BYTES: Int = StatePackets.MAX_BYTES

  /** The longest reliable message: a command at its longest with room for its frame. */
  public const val MAX_RELIABLE_BYTES: Int = WorldCodec.MAX_COMMAND_BYTES + 64

  /** What [Connection.receive] returns when no message is waiting. */
  public const val NO_MESSAGE: Int = -1

  /** What [Connection.closeCode] reads while the connection is open. */
  public const val STILL_OPEN: Int = -1

  /** The close code of a connection closed with no reason to give. */
  public const val CLOSED_PLAINLY: Int = 0

  /**
   * The largest close code: what an application may pass through a socket library's own range of
   * reason codes.
   */
  public const val MAX_CLOSE_CODE: Int = 999
}
