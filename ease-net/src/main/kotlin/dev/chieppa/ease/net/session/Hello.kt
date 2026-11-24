package dev.chieppa.ease.net.session

import dev.chieppa.ease.commands.WorldCodec
import dev.chieppa.ease.ecs.replication.WireBits
import dev.chieppa.ease.net.channel.ChannelKind
import dev.chieppa.ease.net.channel.Connection

private const val BYTE_VALUE = 0xFFL

/**
 * The first message each end sends on the reliable ordered channel: the 8-byte hash of the types
 * its codec numbers on the wire ([WorldCodec.typesHash]). Each end refuses a peer whose hash
 * differs, since their snapshots and commands would be misread.
 */
internal object Hello {
  /** The message's length in bytes. */
  const val BYTES: Int = Long.SIZE_BYTES

  /** Sends [codec]'s hello on [connection]. */
  fun send(codec: WorldCodec, connection: Connection) {
    val bytes = ByteArray(BYTES)
    for (index in 0 until BYTES) {
      bytes[index] = (codec.typesHash ushr (index * WireBits.BYTE_BITS)).toByte()
    }
    connection.send(ChannelKind.RELIABLE_ORDERED, bytes, 0, BYTES)
  }

  /** Whether the first [length] bytes of [bytes] are a hello for [codec]'s types. */
  fun matches(codec: WorldCodec, bytes: ByteArray, length: Int): Boolean {
    if (length != BYTES) return false
    var hash = 0L
    for (index in 0 until BYTES) {
      hash = hash or ((bytes[index].toLong() and BYTE_VALUE) shl (index * WireBits.BYTE_BITS))
    }
    return hash == codec.typesHash
  }
}
