package dev.chieppa.ease.net

import com.github.quillraven.fleks.World
import dev.chieppa.ease.commands.WorldCodec
import dev.chieppa.ease.ecs.replication.BitWriter
import dev.chieppa.ease.net.channel.ChannelKind
import dev.chieppa.ease.net.command.CommandExecutor
import dev.chieppa.ease.net.command.CommandFrames
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * A client cannot send a command the host would refuse as malformed: it throws locally and the
 * connection stays open. And the host never trusts the tick a sender stamps on a command.
 */
class SenderLimitsTest {
  private val applied = mutableListOf<Long>()
  private var host: World? = null
  private val session =
    Session(
      entities = 1,
      executor =
        CommandExecutor { command, _, tick ->
          command.apply(checkNotNull(host))
          applied += tick
        },
    )

  init {
    host = session.host
  }

  private fun assertNothingSent(command: Note) {
    session.step()
    assertFailsWith<IllegalArgumentException> { session.clientSession.sendCommand(1, command) }
    session.step()

    assertTrue(session.clientSession.isOpen)
    assertNull(session.peer.closeReason)
    assertTrue(applied.isEmpty())
  }

  @Test
  fun `a command just past the longest a peer may send throws locally`() {
    assertNothingSent(Note("x".repeat(WorldCodec.MAX_COMMAND_BYTES)))
  }

  @Test
  fun `a command far past the longest throws locally`() {
    assertNothingSent(Note("x".repeat(WorldCodec.MAX_COMMAND_BYTES * 2)))
  }

  @Test
  fun `a command with too long a collection throws locally`() {
    assertNothingSent(Note("", List(LONG_LIST) { 0 }))
  }

  @Test
  fun `a command just inside the longest is sent and applied`() {
    session.step()
    session.clientSession.sendCommand(1, Note("x".repeat(WorldCodec.MAX_COMMAND_BYTES - SLACK)))
    session.step()

    assertEquals(1, applied.size)
  }

  @Test
  fun `a command applies at the host's tick, whatever tick its sender stamped`() {
    repeat(STEPS) { session.step() }
    val writer = BitWriter(CommandFrames.MAX_BYTES)
    val commands = session.codec.commandWriter()
    val length = CommandFrames.write(writer, commands, Long.MAX_VALUE, Push(session.idOf(0), 1f))
    session.link.clientEnd.send(ChannelKind.RELIABLE_ORDERED, writer.buffer, 0, length)
    val hostTick = session.tick
    session.step()

    assertEquals(listOf(hostTick), applied)
  }

  private companion object {
    const val LONG_LIST = 4_097
    const val SLACK = 16
    const val STEPS = 3
  }
}
