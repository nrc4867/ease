package dev.chieppa.ease.net.snapshot

import dev.chieppa.ease.net.channel.Channels

/**
 * The layout of a snapshot message: the host's tick, then the last input tick it processed for the
 * receiving peer plus one (0 when none), both variable-length and padded to a byte, then a state
 * packet whose fields are those the peer may not hold yet.
 */
internal object Snapshots {
  /** The longest header: two variable-length numbers of at most ten bytes each. */
  const val MAX_HEADER_BYTES: Int = 20

  /** The bytes left for the state packet in one unreliable message. */
  const val STATE_BYTES: Int = Channels.MAX_UNRELIABLE_BYTES - MAX_HEADER_BYTES

  /** A tick no snapshot or input carries. */
  const val NO_TICK: Long = -1L

  /** How many sent snapshots a peer's acknowledgements can still reach. */
  const val SENT_RING: Int = 64
}
