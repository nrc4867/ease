package dev.chieppa.ease.net

import dev.chieppa.ease.net.channel.ChannelKind
import dev.chieppa.ease.net.channel.Channels
import dev.chieppa.ease.net.channel.Connection

/** Wraps [inner] and keeps a copy of every message received through it, on every channel. */
internal class RecordingConnection(private val inner: Connection) : Connection by inner {
  /** Every message received, oldest first, with its channel. */
  val received = mutableListOf<Pair<ChannelKind, ByteArray>>()

  override fun receive(kind: ChannelKind, target: ByteArray): Int {
    val length = inner.receive(kind, target)
    if (length != Channels.NO_MESSAGE) received += kind to target.copyOf(length)
    return length
  }

  /** How many messages arrived on [kind]. */
  fun count(kind: ChannelKind): Int = received.count { it.first == kind }

  /**
   * Whether any message received holds the 32 bits of [value] at any bit position, as a field of a
   * state packet or a spawn would carry it.
   */
  fun carries(value: Float): Boolean {
    val pattern = value.toRawBits()
    return received.any { (_, bytes) -> holdsBits(bytes, pattern) }
  }

  private fun holdsBits(bytes: ByteArray, pattern: Int): Boolean {
    val bits = bytes.size * Byte.SIZE_BITS
    return (0..bits - Int.SIZE_BITS).any { start -> bitsAt(bytes, start) == pattern }
  }

  /** The 32 bits from bit [start], lowest bits first, as the bit codec writes them. */
  private fun bitsAt(bytes: ByteArray, start: Int): Int {
    var result = 0
    for (bit in 0 until Int.SIZE_BITS) {
      val at = start + bit
      val set = (bytes[at / Byte.SIZE_BITS].toInt() ushr (at % Byte.SIZE_BITS)) and 1
      result = result or (set shl bit)
    }
    return result
  }
}
