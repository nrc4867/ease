package dev.chieppa.ease.net.command

import com.github.quillraven.fleks.World
import dev.chieppa.ease.commands.Command
import dev.chieppa.ease.commands.CommandRejected
import dev.chieppa.ease.commands.WorldCodec
import dev.chieppa.ease.commands.permissionRefusal
import dev.chieppa.ease.ecs.replication.BitReader
import dev.chieppa.ease.ecs.replication.PeerId

private const val BYTE_BITS = 8
private const val OVER_LIMIT = "Over the rate limit"
private const val NO_HEADER = -1

/**
 * The authority's checks on the commands one peer sends, in order: the connection's rate limit, the
 * shape (the frame and the command decoder), the command type's rate limit, permission from scope
 * and ownership, then the command's own rules. A command refused at any layer changes nothing and
 * is reported to [refusals]; a malformed one, or too many strikes, closes the connection. Checking
 * allocates nothing but the decoded command.
 */
internal class CommandIntake(
  private val world: World,
  private val codec: WorldCodec,
  private val peer: PeerId,
  private val limits: RateLimits,
  private val executor: CommandExecutor,
  private val refusals: CommandRefusals,
) {
  private val header = BitReader()
  private val reader = codec.commandReader()
  private val connection = RateBucket(limits.perConnection)
  private val byType = typeBuckets(codec, limits)
  private var strikes = 0L
  private var lastStrikeTick = 0L
  private var sentTick = 0L

  /** Whether the connection must close: a malformed command or too many strikes. */
  var mustClose: Boolean = false
    private set

  /** Whether the connection must close because a message was not a command. */
  var malformed: Boolean = false
    private set

  /** Checks and applies the command in the first [length] bytes of [bytes], at [hostTick]. */
  fun receive(bytes: ByteArray, length: Int, hostTick: Long) {
    val command =
      if (connection.take(hostTick)) decode(bytes, length) else null.also { strike(hostTick) }
    if (command != null) check(command, hostTick)
  }

  private fun check(command: Command, hostTick: Long) {
    if (!byType[codec.commandTypeId(command)].take(hostTick)) return strike(hostTick)
    val permission = permissionRefusal(world, command, peer)
    val rules = if (permission == null) command.validate(world) else null
    when {
      permission != null -> refusals.refused(peer, CommandLayer.PERMISSION, permission)
      rules != null -> refusals.refused(peer, CommandLayer.RULES, rules)
      else -> execute(command)
    }
  }

  private fun execute(command: Command) {
    try {
      executor.execute(command, peer, sentTick)
    } catch (rejected: CommandRejected) {
      refusals.refused(peer, CommandLayer.RULES, rejected.message ?: rejected.toString())
    }
  }

  private fun decode(bytes: ByteArray, length: Int): Command? {
    val fits = length in 0..minOf(CommandFrames.MAX_BYTES, bytes.size)
    val offset = if (fits) readHeader(bytes, length) else NO_HEADER
    if (offset == NO_HEADER) return malformed("A command message of $length bytes without a tick")
    return reader.read(bytes, offset, length - offset)
      ?: malformed(reader.detail ?: reader.refusal.toString())
  }

  /** Reads the sender's tick and returns where the command starts, or [NO_HEADER]. */
  private fun readHeader(bytes: ByteArray, length: Int): Int {
    header.reset(bytes, length)
    val tick = header.readVarLong()
    if (!header.ok || tick < 0) return NO_HEADER
    sentTick = tick
    return (header.bitPosition + BYTE_BITS - 1) / BYTE_BITS
  }

  private fun malformed(reason: String): Command? {
    mustClose = true
    malformed = true
    refusals.refused(peer, CommandLayer.SHAPE, reason)
    return null
  }

  private fun strike(hostTick: Long) {
    strikes = maxOf(0L, strikes - (hostTick - lastStrikeTick)) + 1
    lastStrikeTick = hostTick
    if (strikes > limits.strikesToClose) mustClose = true
    refusals.refused(peer, CommandLayer.RATE, OVER_LIMIT)
  }

  private companion object {
    /** A bucket per registered command type, under its own limit or the default one. */
    fun typeBuckets(codec: WorldCodec, limits: RateLimits): Array<RateBucket> {
      val perType = Array(codec.commandTypeCount) { limits.perType }
      limits.byType.forEach { (name, limit) ->
        val id = codec.commandTypeId(name)
        require(id >= 0) { "No command type is registered as $name" }
        perType[id] = limit
      }
      return Array(perType.size) { RateBucket(perType[it]) }
    }
  }
}
