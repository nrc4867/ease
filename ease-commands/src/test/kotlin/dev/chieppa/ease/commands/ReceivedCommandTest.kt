package dev.chieppa.ease.commands

import com.github.quillraven.fleks.Snapshot
import dev.chieppa.ease.commands.builtin.Restore
import dev.chieppa.ease.commands.builtin.SetTransform
import dev.chieppa.ease.commands.builtin.Spawn
import dev.chieppa.ease.ecs.LocalTransform
import dev.chieppa.ease.ecs.replication.BitWriter
import dev.chieppa.ease.ecs.replication.NetworkId
import dev.chieppa.ease.math.Transform
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/**
 * Commands received from a peer as hostile bytes: registered commands are accepted; random,
 * truncated, corrupted, unknown and oversized ones are refused, never thrown. The generator is
 * seeded, so every run feeds the same bytes.
 */
class ReceivedCommandTest {
  private val codec = worldCodec()
  private val random = Random(SEED)
  private val commands: List<Command> =
    listOf(
      SetTransform(NetworkId(3), Transform.IDENTITY),
      Spawn(listOf(LocalTransform.of(Transform.IDENTITY))),
      Restore(NetworkId(5), Snapshot(listOf(LocalTransform()), emptyList())),
    )

  @Test
  fun `a registered command is accepted`() {
    val received = codec.receiveCommand(codec.commandToBinary(commands[0]))

    val accepted = assertIs<ReceivedCommand.Accepted>(received)
    assertEquals(codec.commandToJson(commands[0]), codec.commandToJson(accepted.command))
  }

  @Test
  fun `a command type the codec does not know is refused`() {
    val withNudge = worldCodec { command(Nudge::class, Nudge.serializer()) }
    val bytes = withNudge.commandToBinary(Nudge(NetworkId(1), 2f))

    assertRefused(CommandRefusal.MALFORMED, codec.receiveCommand(bytes))
  }

  @Test
  fun `a declared length past the end of the bytes is refused`() {
    val writer = BitWriter(SMALL)
    writer.writeVarLong(codec.commandTypeId(commands[1]).toLong())
    writer.writeVarLong(HUGE_SIZE)
    writer.padToByte()

    assertRefused(CommandRefusal.MALFORMED, codec.receiveCommand(writer.buffer.copyOf(SMALL)))
  }

  @Test
  fun `a command type id past the registered ones is refused`() {
    val writer = BitWriter(SMALL)
    writer.writeVarLong(codec.commandTypeCount.toLong())

    assertRefused(CommandRefusal.MALFORMED, codec.receiveCommand(writer.buffer.copyOf(1)))
  }

  @Test
  fun `bytes left over after a command are refused`() {
    val bytes = codec.commandToBinary(commands[0])

    assertRefused(CommandRefusal.MALFORMED, codec.receiveCommand(bytes.copyOf(bytes.size + 1)))
  }

  @Test
  fun `oversized bytes are refused unread`() {
    val bytes = ByteArray(WorldCodec.MAX_COMMAND_BYTES + 1)

    assertRefused(CommandRefusal.TOO_LONG, codec.receiveCommand(bytes))
  }

  @Test
  fun `every truncation of a command is refused`() {
    commands.forEach { command ->
      val bytes = codec.commandToBinary(command)
      for (cut in 0 until bytes.size) {
        assertRefused(CommandRefusal.MALFORMED, codec.receiveCommand(bytes.copyOf(cut)))
      }
    }
  }

  @Test
  fun `random and corrupted bytes are refused or accepted, never thrown`() {
    repeat(RANDOM_PACKETS) { codec.receiveCommand(random.nextBytes(random.nextInt(MAX_RANDOM))) }
    repeat(RANDOM_PACKETS) {
      val bytes = codec.commandToBinary(commands[random.nextInt(commands.size)])
      repeat(1 + random.nextInt(MAX_FLIPS)) {
        val index = random.nextInt(bytes.size)
        bytes[index] = (bytes[index].toInt() xor (1 shl random.nextInt(BYTE_BITS))).toByte()
      }
      codec.receiveCommand(bytes)
    }
  }

  private fun assertRefused(reason: CommandRefusal, received: ReceivedCommand) {
    assertEquals(reason, assertIs<ReceivedCommand.Refused>(received).reason)
  }

  private companion object {
    const val SEED = 7_331L
    const val RANDOM_PACKETS = 5_000
    const val MAX_RANDOM = 256
    const val MAX_FLIPS = 4
    const val BYTE_BITS = 8

    const val SMALL = 8
    const val HUGE_SIZE = 1_000_000L
  }
}
