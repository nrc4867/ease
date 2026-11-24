package dev.chieppa.ease.commands

import dev.chieppa.ease.commands.wire.WireDecoder
import kotlinx.serialization.PolymorphicSerializer

/**
 * Reads commands received from a peer, treating every byte as hostile, allocating nothing but the
 * command it reads. Bytes past [WorldCodec.MAX_COMMAND_BYTES] are refused unread, only a registered
 * command type can be named, and malformed bytes give a refusal, never an exception. Each receiver
 * keeps its own, from [WorldCodec.commandReader]; one is not shared between threads.
 */
public class CommandReader internal constructor(private val decoder: WireDecoder) {
  private val serializer = PolymorphicSerializer(Command::class)

  /** Why the last [read] returned null, or null when it read a command. */
  public var refusal: CommandRefusal? = null
    private set

  /** The decoder's account of the last refusal, for logs. */
  public var detail: String? = null
    private set

  /** The command the [length] bytes of [bytes] from [offset] hold, or null with a [refusal]. */
  public fun read(bytes: ByteArray, offset: Int, length: Int): Command? {
    refusal = null
    detail = null
    return when {
      length > WorldCodec.MAX_COMMAND_BYTES -> refuse(CommandRefusal.TOO_LONG, "$length bytes")
      offset < 0 || length < 0 || offset + length > bytes.size ->
        refuse(CommandRefusal.MALFORMED, "Bytes outside the array")
      else -> decode(bytes, offset, length)
    }
  }

  private fun decode(bytes: ByteArray, offset: Int, length: Int): Command? {
    decoder.reader.reset(bytes, offset, length)
    // A command's own constructor may throw on values it cannot hold, and no exception may pass
    // the connection boundary.
    @Suppress("TooGenericExceptionCaught")
    val command =
      try {
        decoder.read(serializer)
      } catch (malformed: RuntimeException) {
        refuse(CommandRefusal.MALFORMED, malformed.message ?: malformed.toString())
      }
    val leftOver = command != null && decoder.reader.remainingBits >= Byte.SIZE_BITS
    return if (leftOver) refuse(CommandRefusal.MALFORMED, "Bytes left over after the command")
    else command
  }

  private fun refuse(reason: CommandRefusal, why: String): Command? {
    refusal = reason
    detail = why
    return null
  }
}
