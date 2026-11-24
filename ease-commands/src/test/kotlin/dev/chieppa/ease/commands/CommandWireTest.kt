package dev.chieppa.ease.commands

import dev.chieppa.ease.ecs.replication.BitWriter
import dev.chieppa.ease.ecs.replication.NetworkId
import dev.chieppa.ease.testing.AllocationBudget
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The binary form carries every kind of value a command holds, writes into reused buffers without
 * allocating, and refuses what no writer produces.
 */
class CommandWireTest {
  private val codec = worldCodec {
    command(Sampler::class, Sampler.serializer())
    command(Nudge::class, Nudge.serializer())
  }
  private val writer = BitWriter(WorldCodec.MAX_COMMAND_BYTES)
  private val commands = codec.commandWriter()
  private val reader = codec.commandReader()

  private fun sampler(depth: Int): Sampler =
    Sampler(
      flag = true,
      small = Byte.MIN_VALUE,
      medium = Short.MAX_VALUE,
      letter = 'é',
      count = Int.MIN_VALUE,
      big = Long.MIN_VALUE,
      ratio = -0.25f,
      precise = Math.PI,
      label = "lantern ☃",
      shade = Shade.BRIGHT,
      maybe = -7,
      missing = null,
      list = listOf(0, -1, Int.MAX_VALUE),
      map = mapOf("a" to Long.MAX_VALUE, "b" to -1L),
      nested = if (depth == 0) emptyList() else listOf(sampler(depth - 1)),
    )

  private fun roundTrip(command: Command): Command? {
    writer.reset()
    commands.write(command, writer)
    writer.padToByte()
    return reader.read(writer.buffer, 0, writer.byteLength)
  }

  @Test
  fun `every kind of value round-trips`() {
    val command = sampler(depth = 2)

    assertEquals(command, roundTrip(command))
  }

  @Test
  fun `the log's bytes read back to the same command`() {
    val command = sampler(depth = 1)

    assertEquals(command, codec.commandFromBinary(codec.commandToBinary(command)))
  }

  @Test
  fun `a float that is not finite is refused`() {
    val command = sampler(depth = 0).copy(ratio = Float.NaN)

    assertNull(roundTrip(command))
    assertEquals(CommandRefusal.MALFORMED, reader.refusal)
  }

  @Test
  fun `values nested past a peer's limit cannot be written`() {
    val command = sampler(depth = DEEP)

    assertFailsWith<IllegalArgumentException> { roundTrip(command) }
  }

  @Test
  fun `writing a command without collections into a reused buffer allocates nothing`() {
    // A non-empty list or map costs the iterator its kotlinx serializer takes.
    val command = sampler(depth = 0).copy(list = emptyList(), map = emptyMap())
    val write = Runnable {
      writer.reset()
      commands.write(command, writer)
    }

    AllocationBudget.assertWithin(0, write, WARMUP, MEASURED)
  }

  @Test
  fun `random bytes are refused or read, never thrown`() {
    val random = Random(SEED)
    repeat(RANDOM_MESSAGES) {
      val bytes = random.nextBytes(random.nextInt(MAX_RANDOM))
      reader.read(bytes, 0, bytes.size)
    }
  }

  @Test
  fun `a command names its type by number, so it is shorter than its JSON`() {
    val nudge = Nudge(NetworkId(1), 2f)

    assertEquals(nudge.distance, (roundTrip(nudge) as Nudge).distance)
    assertTrue(writer.byteLength < codec.commandToJson(nudge).length / 4)
  }

  private companion object {
    const val DEEP = 40
    const val WARMUP = 1_000
    const val MEASURED = 1_000
    const val SEED = 5_150L
    const val RANDOM_MESSAGES = 5_000
    const val MAX_RANDOM = 200
  }
}
