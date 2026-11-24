package dev.chieppa.ease.net.command

import dev.chieppa.ease.commands.Command
import dev.chieppa.ease.commands.CommandWriter
import dev.chieppa.ease.commands.WorldCodec
import dev.chieppa.ease.ecs.replication.BitWriter
import dev.chieppa.ease.ecs.replication.BitWriterFull

/**
 * The layout of a command message on the reliable ordered channel: the sender's tick as a
 * variable-length number padded to a byte, then the command in the codec's binary form.
 */
internal object CommandFrames {
  /** The longest header: one variable-length number. */
  const val MAX_HEADER_BYTES: Int = 10

  /** The longest command message. */
  const val MAX_BYTES: Int = MAX_HEADER_BYTES + WorldCodec.MAX_COMMAND_BYTES

  /**
   * Writes [command] stamped with [tick] into [writer], which it empties, and returns the length.
   * Throws [IllegalArgumentException] for a command the host would refuse as malformed: longer than
   * [WorldCodec.MAX_COMMAND_BYTES], nested too deep or with too long a collection.
   */
  fun write(writer: BitWriter, commands: CommandWriter, tick: Long, command: Command): Int {
    writer.reset()
    writer.writeVarLong(tick)
    writer.padToByte()
    try {
      commands.write(command, writer)
    } catch (full: BitWriterFull) {
      throw IllegalArgumentException(
        "A command longer than ${WorldCodec.MAX_COMMAND_BYTES} bytes",
        full,
      )
    }
    writer.padToByte()
    return writer.byteLength
  }
}
