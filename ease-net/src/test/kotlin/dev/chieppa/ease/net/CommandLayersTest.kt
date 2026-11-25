package dev.chieppa.ease.net

import dev.chieppa.ease.commands.builtin.SetTransform
import dev.chieppa.ease.math.Quat
import dev.chieppa.ease.math.Transform
import dev.chieppa.ease.math.Vec3
import dev.chieppa.ease.net.channel.ChannelKind
import dev.chieppa.ease.net.command.CommandLayer
import dev.chieppa.ease.net.session.CloseReason
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Commands from a client pass three layers on the host: shape, permission, rules. One that fails
 * any of them changes nothing in either world and is reported with the peer, layer and reason.
 */
class CommandLayersTest {
  private val session = Session(entities = 2, owned = 1)

  init {
    session.step()
  }

  /** Steps once, then asserts both worlds are as they were and [layer] refused one command. */
  private fun assertRefusedAt(layer: CommandLayer) {
    val before = session.states()
    session.step()
    session.step()

    assertEquals(before, session.states())
    assertEquals(listOf(layer), session.refused.map { it.first })
  }

  @Test
  fun `a command that passes every layer applies`() {
    session.clientSession.sendCommand(session.tick, Push(session.idOf(0), 1f))
    session.step()
    session.step()

    assertTrue(session.refused.isEmpty())
    assertEquals(1f to 1f, session.xs(0))
  }

  @Test
  fun `bytes that are not a command fail the shape layer and close the connection`() {
    val garbage = byteArrayOf(0, 0x7F, 0x7F, 0x7F)
    session.link.clientEnd.send(ChannelKind.RELIABLE_ORDERED, garbage, 0, garbage.size)

    assertRefusedAt(CommandLayer.SHAPE)
    assertFalse(session.peer.isOpen)
    assertEquals(CloseReason.MALFORMED_COMMAND, session.peer.closeReason)
  }

  @Test
  fun `a command for an entity the client does not own fails the permission layer`() {
    session.clientSession.sendCommand(session.tick, Push(session.idOf(1), 1f))

    assertRefusedAt(CommandLayer.PERMISSION)
    assertTrue(session.peer.isOpen)
  }

  @Test
  fun `a command only the host may send fails the permission layer`() {
    val moved = Transform(Vec3(5f, 0f, 0f), Quat.IDENTITY, Vec3.ONE)
    session.clientSession.sendCommand(session.tick, SetTransform(session.idOf(0), moved))

    assertRefusedAt(CommandLayer.PERMISSION)
  }

  @Test
  fun `a command its own validate refuses fails the rules layer`() {
    session.clientSession.sendCommand(session.tick, Push(session.idOf(0), Push.MAX_STEP * 2))

    assertRefusedAt(CommandLayer.RULES)
    val reason = session.refused.single().second
    assertTrue(reason.contains("at most"))
  }

  @Test
  fun `refusals leave later commands to apply`() {
    session.clientSession.sendCommand(session.tick, Push(session.idOf(1), 1f))
    session.clientSession.sendCommand(session.tick, Push(session.idOf(0), 1f))
    session.step()
    session.step()

    assertEquals(1f to 1f, session.xs(0))
    assertNull(session.peer.closeReason)
  }
}
