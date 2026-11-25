package dev.chieppa.ease.net.transport

import dev.chieppa.ease.net.channel.ChannelKind

/**
 * One message a simulated link holds: its bytes, channel, the tick it is due at (or [WAITING] for
 * bandwidth), and the order it was sent in. Slots are reused, their buffers grown only when a
 * message does not fit.
 */
internal class FlightSlot {
  var bytes: ByteArray = ByteArray(0)
  var kind: ChannelKind = ChannelKind.UNRELIABLE
  var length: Int = 0
  var due: Long = WAITING
  var order: Long = 0L
  var used: Boolean = false

  /** Whether the slot holds a message waiting for bandwidth. */
  val isWaiting: Boolean
    get() = used && due == WAITING

  /** Whether the slot holds a sent message due at or before [tick]. */
  fun isDueBy(tick: Long): Boolean = used && due != WAITING && due <= tick

  /** Whether this slot's message arrives before [other]'s: due first, or sent first on a tie. */
  fun arrivesBefore(other: FlightSlot?): Boolean =
    other == null || due < other.due || (due == other.due && order < other.order)

  companion object {
    /** The [due] of a message still waiting for bandwidth. */
    const val WAITING = Long.MIN_VALUE
  }
}
