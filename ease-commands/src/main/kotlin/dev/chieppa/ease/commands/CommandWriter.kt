package dev.chieppa.ease.commands

import dev.chieppa.ease.commands.wire.WireEncoder
import dev.chieppa.ease.ecs.replication.BitWriter
import kotlinx.serialization.PolymorphicSerializer

/**
 * Writes commands in the binary form into a reused [BitWriter], allocating nothing. Each sender
 * keeps its own, from [WorldCodec.commandWriter]; one is not shared between threads.
 */
public class CommandWriter internal constructor(private val encoder: WireEncoder) {
  private val serializer = PolymorphicSerializer(Command::class)

  /**
   * Writes [command] after what [writer] already holds. Throws when its type is not registered or
   * it does not fit [writer].
   */
  public fun write(command: Command, writer: BitWriter) {
    encoder.write(serializer, command, writer)
  }
}
