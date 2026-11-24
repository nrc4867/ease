package dev.chieppa.ease.commands

import dev.chieppa.ease.ecs.phasedWorld
import dev.chieppa.ease.ecs.replication.BitWriter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The log and undo history hold commands far larger than a peer may send, and read back all they
 * write; a peer's limits apply only to what crosses the network, and a sender learns of a command
 * past them before anything is sent.
 */
class LargeCommandTest {
  private val codec = worldCodec { command(Paint::class, Paint.serializer()) }
  private val world = phasedWorld {}
  private val history = CommandHistory(world, codec)

  @Test
  fun `a change whose inverse is past a peer's limits is undone and redone`() {
    history.execute(Paint(CELLS), 1, CommandOrigin.USER)

    assertTrue(history.undo(2, CommandOrigin.USER))
    assertTrue(history.redo(3, CommandOrigin.USER))
    assertEquals(3, history.log.all.size)
  }

  @Test
  fun `the log reads back a command longer than a peer may send`() {
    val large = Paint(0, List(CELLS) { it * BIG })
    val bytes = codec.commandToBinary(large)

    assertTrue(bytes.size > WorldCodec.MAX_COMMAND_BYTES)
    assertEquals(large.cells, (codec.commandFromBinary(bytes) as Paint).cells)
  }

  @Test
  fun `a peer's reader refuses what the log holds`() {
    val bytes = codec.commandToBinary(Paint(0, List(CELLS) { it }))

    assertIs<ReceivedCommand.Refused>(codec.receiveCommand(bytes))
  }

  @Test
  fun `a command for a peer past its limits throws before anything is written`() {
    val writer = BitWriter(WorldCodec.MAX_COMMAND_BYTES * 2)
    writer.writeBits(1, 1)

    assertFailsWith<IllegalArgumentException> {
      codec.commandWriter().write(Paint(0, List(CELLS) { it }), writer)
    }
    assertEquals(1, writer.bitPosition)
  }

  @Test
  fun `bytes whose offset and length overflow are refused, never thrown`() {
    val reader = codec.commandReader()

    assertNull(reader.read(ByteArray(SMALL), Int.MAX_VALUE, SMALL))
    assertEquals(CommandRefusal.MALFORMED, reader.refusal)
  }

  private companion object {
    const val CELLS = 5_000
    const val BIG = 1_000
    const val SMALL = 8
  }
}
