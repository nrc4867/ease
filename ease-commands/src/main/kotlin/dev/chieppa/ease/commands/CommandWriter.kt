package dev.chieppa.ease.commands

import dev.chieppa.ease.commands.wire.Wire
import dev.chieppa.ease.commands.wire.WireEncoder
import dev.chieppa.ease.ecs.replication.BitWriter
import dev.chieppa.ease.ecs.replication.WireBits
import kotlinx.serialization.PolymorphicSerializer

/**
 * Writes commands in the binary form into a reused [BitWriter], allocating nothing. A command a
 * peer's reader would refuse, too long, nested too deep or with too long a collection, throws
 * instead, so a sender learns of it before anything is sent. Each sender keeps its own, from
 * [WorldCodec.commandWriter]; one is not shared between threads.
 */
public class CommandWriter
internal constructor(private val encoder: WireEncoder, private val limits: Wire) {
  private val serializer = PolymorphicSerializer(Command::class)

  /**
   * Writes [command] after what [writer] already holds, starting on a byte. Throws
   * [IllegalArgumentException], leaving [writer] as it was, when a reader would refuse it; throws
   * when its type is not registered or it does not fit [writer].
   */
  public fun write(command: Command, writer: BitWriter) {
    val start = writer.bitPosition
    try {
      encoder.write(serializer, command, writer)
    } catch (refused: IllegalArgumentException) {
      writer.rewind(start)
      throw refused
    }
    val bytes = WireBits.bytesFor(writer.bitPosition - start)
    if (bytes > limits.maxBytes) {
      writer.rewind(start)
      throw IllegalArgumentException("A $bytes-byte command is longer than ${limits.maxBytes}")
    }
  }
}
