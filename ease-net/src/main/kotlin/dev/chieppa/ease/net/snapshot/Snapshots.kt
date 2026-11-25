package dev.chieppa.ease.net.snapshot

import dev.chieppa.ease.net.channel.Channels

/**
 * The layout of a snapshot message: the host's tick, the last input tick it processed for the
 * receiving peer plus one (0 when none), the number of the latest spawn among the entities it
 * carries (counting every spawn and despawn sent to that peer from 1, 0 when it carries none), the
 * input tick the peer's slack was measured on plus one (0 when no input came since the last
 * snapshot), and that slack zigzag-encoded, all variable-length and padded to a byte; then a state
 * packet whose fields are those the peer may not hold yet. A client applies a snapshot once it has
 * applied at least that many spawns and despawns, so a snapshot never names an entity the client
 * has not been told about, and a lagging spawn holds back only the snapshots that carry its entity.
 */
internal object Snapshots {
  /** The longest header: five variable-length numbers of at most ten bytes each. */
  const val MAX_HEADER_BYTES: Int = 50

  /** The bytes left for the state packet in one unreliable message. */
  const val STATE_BYTES: Int = Channels.MAX_UNRELIABLE_BYTES - MAX_HEADER_BYTES

  /** A tick no snapshot or input carries. */
  const val NO_TICK: Long = -1L

  /** How many sent snapshots a peer's acknowledgements can still reach. */
  const val SENT_RING: Int = 64

  /** The largest slack a client takes from a snapshot either way: about 580 years of ticks. */
  const val MAX_SLACK: Long = 1L shl 40

  /** [value] as a number that is not negative, small when [value] is near 0. */
  fun zigzag(value: Long): Long = (value shl 1) xor (value shr (Long.SIZE_BITS - 1))

  /** The number [zigzag] made [encoded] from. */
  fun unzigzag(encoded: Long): Long = (encoded ushr 1) xor -(encoded and 1L)
}
