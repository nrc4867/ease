package dev.chieppa.ease.net.channel

import dev.chieppa.ease.ecs.replication.WireBits

private const val LENGTH_BYTES = 4
private const val BYTE_VALUE = 0xFF

/**
 * Byte messages in arrival order, in one ring of bytes, each behind its four-byte length. It grows
 * when a message does not fit and allocates nothing otherwise.
 */
internal class MessageQueue(initialBytes: Int) {
  private var ring = ByteArray(initialBytes)
  private var head = 0
  private var used = 0

  /** Whether no message is waiting. */
  val isEmpty: Boolean
    get() = used == 0

  /** Adds [length] bytes of [bytes] from [offset] as one message. */
  fun push(bytes: ByteArray, offset: Int, length: Int) {
    if (used + LENGTH_BYTES + length > ring.size) grow(used + LENGTH_BYTES + length)
    var lengthBits = length
    repeat(LENGTH_BYTES) {
      put((lengthBits and BYTE_VALUE).toByte())
      lengthBits = lengthBits ushr WireBits.BYTE_BITS
    }
    val tail = (head + used) % ring.size
    val first = minOf(length, ring.size - tail)
    System.arraycopy(bytes, offset, ring, tail, first)
    System.arraycopy(bytes, offset + first, ring, 0, length - first)
    used += length
  }

  /** Moves the oldest message into [target] and returns its length, or -1 when there is none. */
  fun pop(target: ByteArray): Int {
    if (used == 0) return Channels.NO_MESSAGE
    var length = 0
    for (index in 0 until LENGTH_BYTES) {
      length = length or ((take().toInt() and BYTE_VALUE) shl (index * WireBits.BYTE_BITS))
    }
    check(length <= target.size) { "A $length-byte message does not fit ${target.size} bytes" }
    val first = minOf(length, ring.size - head)
    System.arraycopy(ring, head, target, 0, first)
    System.arraycopy(ring, 0, target, first, length - first)
    head = (head + length) % ring.size
    used -= length
    return length
  }

  /** Drops every waiting message. */
  fun clear() {
    head = 0
    used = 0
  }

  private fun put(byte: Byte) {
    ring[(head + used) % ring.size] = byte
    used++
  }

  private fun take(): Byte {
    val byte = ring[head]
    head = (head + 1) % ring.size
    used--
    return byte
  }

  private fun grow(needed: Int) {
    var size = ring.size * 2
    while (size < needed) size *= 2
    val grown = ByteArray(size)
    val first = minOf(used, ring.size - head)
    System.arraycopy(ring, head, grown, 0, first)
    System.arraycopy(ring, 0, grown, first, used - first)
    ring = grown
    head = 0
  }
}
