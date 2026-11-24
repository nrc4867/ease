package dev.chieppa.ease.net

import dev.chieppa.ease.net.channel.ChannelKind
import dev.chieppa.ease.net.input.InputBuffer
import dev.chieppa.ease.net.input.InputFrame
import dev.chieppa.ease.net.input.InputWindow
import dev.chieppa.ease.net.session.ClientSession
import dev.chieppa.ease.net.session.HostSession
import dev.chieppa.ease.net.session.NetSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Input travels every tick on the unreliable channel, each packet repeating the last few ticks, and
 * the host takes only packets inside its tick window.
 */
class InputWindowTest {
  private val settings = NetSettings(window = InputWindow(ahead = AHEAD, behind = BEHIND))
  private val session = Session(entities = 1, settings = settings)
  private val host: HostSession = session.hostSession
  private val client: ClientSession = session.clientSession
  private val inputs: InputBuffer = session.peer.inputs
  private val frame = InputFrame(settings.input)

  private fun send(tick: Long, x: Float) {
    frame.setAxis(0, x)
    client.sendInput(tick, frame)
  }

  @Test
  fun `input inside the window is taken on its own tick`() {
    send(HOST_TICK, HALF)
    host.receive(HOST_TICK)

    assertEquals(HALF, inputs.take(HOST_TICK).axis(0), PRECISION)
    assertEquals(HOST_TICK, inputs.lastProcessedTick)
    assertEquals(0, inputs.droppedPackets)
  }

  @Test
  fun `input stamped too far ahead of the host is dropped`() {
    send(HOST_TICK + AHEAD + 1, HALF)
    host.receive(HOST_TICK)

    assertEquals(1, inputs.droppedPackets)
    assertEquals(0f, inputs.take(HOST_TICK + AHEAD + 1).axis(0))
  }

  @Test
  fun `input stamped too far behind the host is dropped`() {
    send(HOST_TICK - BEHIND - 1, HALF)
    host.receive(HOST_TICK)

    assertEquals(1, inputs.droppedPackets)
    assertEquals(-1L, inputs.lastProcessedTick)
  }

  @Test
  fun `a lost packet's tick arrives in the next one`() {
    frame.setAxis(1, -1f)
    client.sendInput(HOST_TICK, frame)
    session.link.hostEnd.receive(ChannelKind.UNRELIABLE, ByteArray(ChannelKind.UNRELIABLE.maxBytes))
    frame.setAxis(1, 1f)
    client.sendInput(HOST_TICK + 1, frame)
    host.receive(HOST_TICK)

    assertEquals(-1f, inputs.take(HOST_TICK).axis(1))
    assertEquals(1f, inputs.take(HOST_TICK + 1).axis(1))
  }

  @Test
  fun `a tick whose input never arrives holds the last one`() {
    send(HOST_TICK, HALF)
    host.receive(HOST_TICK)
    inputs.take(HOST_TICK)

    assertEquals(HALF, inputs.take(HOST_TICK + 1).axis(0), PRECISION)
    assertEquals(HOST_TICK, inputs.lastProcessedTick)
  }

  @Test
  fun `axes are clamped and buttons kept`() {
    frame.setAxis(0, 3f)
    frame.setButton(2, true)
    client.sendInput(HOST_TICK, frame)
    host.receive(HOST_TICK)
    val taken = inputs.take(HOST_TICK)

    assertEquals(1f, taken.axis(0))
    assertTrue(taken.button(2))
    assertFalse(taken.button(1))
  }

  private companion object {
    const val AHEAD = 5
    const val BEHIND = 5
    const val HOST_TICK = 20L
    const val HALF = 0.5f
    const val PRECISION = 1e-4f
  }
}
