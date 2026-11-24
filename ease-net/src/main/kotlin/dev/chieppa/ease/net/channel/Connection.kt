package dev.chieppa.ease.net.channel

import dev.chieppa.ease.ecs.replication.PeerId

/**
 * One peer's end of a connection to another: three channels, one of each [ChannelKind], carrying
 * byte messages. Sessions talk only to this interface, so an in-process [LocalLink], a loopback
 * transport with a network simulator or a real socket library can sit under them. Sending copies
 * the bytes, and receiving copies them into the caller's buffer, so neither allocates.
 */
public interface Connection {
  /** The peer at the other end. */
  public val remote: PeerId

  /** Whether messages still flow; either end may close it. */
  public val isOpen: Boolean

  /**
   * Sends [length] bytes of [bytes] from [offset] on the [kind] channel. Throws when the message is
   * longer than [ChannelKind.maxBytes]; does nothing once the connection is closed.
   */
  public fun send(kind: ChannelKind, bytes: ByteArray, offset: Int, length: Int)

  /**
   * Copies the next message waiting on the [kind] channel into [target] and returns its length, or
   * returns [Channels.NO_MESSAGE] when none is waiting. [target] holds at least
   * [ChannelKind.maxBytes].
   */
  public fun receive(kind: ChannelKind, target: ByteArray): Int

  /** Closes the connection at both ends; messages not yet received are dropped. */
  public fun close()
}
