package dev.chieppa.ease.net.session

import dev.chieppa.ease.ecs.replication.WireBits
import dev.chieppa.ease.net.channel.ChannelKind
import dev.chieppa.ease.net.channel.Connection
import dev.chieppa.ease.net.join.ContentManifest

private const val BYTE_VALUE = 0xFF
private const val VERSION_AT = 4
private const val TYPES_AT = 6
private const val CONTENT_AT = TYPES_AT + Long.SIZE_BYTES

/**
 * The join handshake: the first message each end sends on the reliable ordered channel, and the
 * only one either takes until the other's matches. In order: the magic bytes `EASE`, the protocol
 * version as 2 bytes, the 8-byte hash of the types the codec numbers on the wire
 * (`WorldCodec.typesHash`), and the [ContentManifest]'s 32-byte digest, little-endian throughout.
 * Each end checks the other's against its own and refuses a mismatch with a typed reason.
 */
internal object JoinHello {
  /** The version of the session protocol this build speaks. */
  const val PROTOCOL_VERSION: Int = 1

  /** The message's length in bytes. */
  const val BYTES: Int = CONTENT_AT + ContentManifest.DIGEST_BYTES

  private val MAGIC =
    byteArrayOf('E'.code.toByte(), 'A'.code.toByte(), 'S'.code.toByte(), 'E'.code.toByte())

  /** Sends the hello for [typesHash] and [content] on [connection]. */
  fun send(connection: Connection, typesHash: Long, content: ContentManifest) {
    val bytes = write(typesHash, content, PROTOCOL_VERSION)
    connection.send(ChannelKind.RELIABLE_ORDERED, bytes, 0, bytes.size)
  }

  /** The hello of a peer speaking [version] with [typesHash] and [content]. */
  fun write(typesHash: Long, content: ContentManifest, version: Int): ByteArray {
    val bytes = ByteArray(BYTES)
    MAGIC.copyInto(bytes)
    bytes[VERSION_AT] = version.toByte()
    bytes[VERSION_AT + 1] = (version ushr WireBits.BYTE_BITS).toByte()
    for (index in 0 until Long.SIZE_BYTES) {
      bytes[TYPES_AT + index] = (typesHash ushr (index * WireBits.BYTE_BITS)).toByte()
    }
    content.digest.copyInto(bytes, CONTENT_AT)
    return bytes
  }

  /**
   * Why the hello in the first [length] bytes of [bytes] cannot join a peer with [typesHash] and
   * [content], or null when it can. A message that is not a hello at all is
   * [CloseReason.MALFORMED_HELLO]; the version is checked before the length, so a later version's
   * longer hello is refused as [CloseReason.PROTOCOL_MISMATCH]. Never throws.
   */
  fun refusal(
    bytes: ByteArray,
    length: Int,
    typesHash: Long,
    content: ContentManifest,
  ): CloseReason? {
    val valid = length >= TYPES_AT && length <= bytes.size
    return when {
      !valid || !hasMagic(bytes) -> CloseReason.MALFORMED_HELLO
      versionOf(bytes) != PROTOCOL_VERSION -> CloseReason.PROTOCOL_MISMATCH
      length != BYTES -> CloseReason.MALFORMED_HELLO
      typesOf(bytes) != typesHash -> CloseReason.TYPES_MISMATCH
      !content.matches(bytes, CONTENT_AT) -> CloseReason.CONTENT_MISMATCH
      else -> null
    }
  }

  private fun hasMagic(bytes: ByteArray): Boolean {
    for (index in MAGIC.indices) if (bytes[index] != MAGIC[index]) return false
    return true
  }

  private fun versionOf(bytes: ByteArray): Int =
    (bytes[VERSION_AT].toInt() and BYTE_VALUE) or
      ((bytes[VERSION_AT + 1].toInt() and BYTE_VALUE) shl WireBits.BYTE_BITS)

  private fun typesOf(bytes: ByteArray): Long {
    var hash = 0L
    for (index in 0 until Long.SIZE_BYTES) {
      val byte = (bytes[TYPES_AT + index].toInt() and BYTE_VALUE).toLong()
      hash = hash or (byte shl (index * WireBits.BYTE_BITS))
    }
    return hash
  }
}
