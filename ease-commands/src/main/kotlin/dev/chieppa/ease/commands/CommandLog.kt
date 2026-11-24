package dev.chieppa.ease.commands

import com.github.quillraven.fleks.World

/**
 * Every command applied to a world, in order, stored in the binary form so later changes to the
 * world can never alter what was logged. Applying the log to a world in the state the first entry
 * saw reproduces every change.
 */
public class CommandLog(private val codec: WorldCodec) {
  private val entries = mutableListOf<CommandLogEntry>()

  /** The entries, oldest first. */
  public val all: List<CommandLogEntry>
    get() = entries

  /** The command [entry] holds, read fresh from its bytes. */
  public fun commandOf(entry: CommandLogEntry): Command = codec.commandFromBinary(entry.encoded)

  /** Applies every logged command to [world], in order. */
  public fun replayOnto(world: World) {
    entries.forEach { commandOf(it).apply(world) }
  }

  internal fun append(tick: Long, origin: CommandOrigin, encoded: ByteArray) {
    entries += CommandLogEntry(tick, origin, encoded)
  }
}

/** One applied command: when, from whom, and the command in the binary form. */
public class CommandLogEntry
internal constructor(
  public val tick: Long,
  public val origin: CommandOrigin,
  internal val encoded: ByteArray,
) {
  /** A copy of the command's bytes, as the log and the network carry them. */
  public fun bytes(): ByteArray = encoded.copyOf()
}
