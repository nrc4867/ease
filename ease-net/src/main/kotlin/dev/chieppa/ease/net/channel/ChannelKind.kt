package dev.chieppa.ease.net.channel

/**
 * How a channel delivers messages. A [Connection] carries one channel of each kind, and a message
 * on one never waits for a message on another.
 */
public enum class ChannelKind(
  /** The longest message the channel carries, in bytes. */
  public val maxBytes: Int
) {
  /** May drop, repeat or reorder messages: state snapshots and per-tick input. */
  UNRELIABLE(Channels.MAX_UNRELIABLE_BYTES),

  /** Delivers every message once, in the order sent: commands and events. */
  RELIABLE_ORDERED(Channels.MAX_RELIABLE_BYTES),

  /** Delivers every message once, in any order: bulk data such as a joining peer's state. */
  RELIABLE_UNORDERED(Channels.MAX_RELIABLE_BYTES),
}
