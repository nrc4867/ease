package dev.chieppa.ease.net

import dev.chieppa.ease.ecs.replication.PeerId
import dev.chieppa.ease.net.channel.ChannelKind
import dev.chieppa.ease.net.channel.Channels
import dev.chieppa.ease.net.channel.Connection
import dev.chieppa.ease.net.transport.LinkConditions
import dev.chieppa.ease.net.transport.LoopbackLink
import dev.chieppa.ease.net.transport.LoopbackTransport
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * The loopback transport's network simulator, driven tick by tick: delays, jitter, loss,
 * duplication and bandwidth caps follow a seeded schedule, and reliable channels stay reliable and
 * ordered whatever the schedule loses.
 */
class LoopbackSimulatorTest {
  private val transport = LoopbackTransport(seed = 3L)
  private val target = ByteArray(Channels.MAX_RELIABLE_BYTES)

  private fun link(conditions: LinkConditions): LoopbackLink =
    transport.connect(PeerId(1), conditions)

  /** Sends message [number] as one byte, [length] bytes long. */
  private fun Connection.sendNumber(kind: ChannelKind, number: Int, length: Int = 1) {
    send(kind, ByteArray(length) { number.toByte() }, 0, length)
  }

  /** Everything waiting on [kind], each message as its first byte. */
  private fun Connection.drain(kind: ChannelKind): List<Int> = buildList {
    while (true) {
      if (receive(kind, target) == Channels.NO_MESSAGE) break
      add(target[0].toInt())
    }
  }

  /** What arrives on [kind] at each of [ticks] ticks, message numbers in arrival order. */
  private fun Connection.arrivalsOver(ticks: Int, kind: ChannelKind): List<List<Int>> =
    List(ticks) {
      transport.update()
      drain(kind)
    }

  @Test
  fun `on a perfect link messages arrive as they are sent, in order`() {
    val link = link(LinkConditions.PERFECT)
    repeat(3) { link.clientEnd.sendNumber(ChannelKind.UNRELIABLE, it) }

    assertEquals(listOf(0, 1, 2), link.hostEnd.drain(ChannelKind.UNRELIABLE))
    assertSame(link.hostEnd, transport.accept())
    assertEquals(null, transport.accept())
  }

  @Test
  fun `latency holds every message for its ticks`() {
    val link = link(LinkConditions(latencyTicks = 3))
    link.hostEnd.sendNumber(ChannelKind.RELIABLE_ORDERED, 7)

    val arrivals = link.clientEnd.arrivalsOver(4, ChannelKind.RELIABLE_ORDERED)

    assertEquals(listOf(emptyList(), emptyList(), listOf(7), emptyList()), arrivals)
  }

  @Test
  fun `jitter reorders unreliable messages but never ordered ones`() {
    val link = link(LinkConditions(latencyTicks = 1, jitterTicks = 4))
    repeat(MESSAGES) {
      link.clientEnd.sendNumber(ChannelKind.UNRELIABLE, it)
      link.clientEnd.sendNumber(ChannelKind.RELIABLE_ORDERED, it)
    }

    val unreliable = mutableListOf<Int>()
    val ordered = mutableListOf<Int>()
    repeat(SETTLE_TICKS) {
      transport.update()
      unreliable += link.hostEnd.drain(ChannelKind.UNRELIABLE)
      ordered += link.hostEnd.drain(ChannelKind.RELIABLE_ORDERED)
    }

    assertEquals((0 until MESSAGES).toList(), ordered)
    assertEquals((0 until MESSAGES).toSet(), unreliable.toSet())
    assertNotEquals((0 until MESSAGES).toList(), unreliable)
  }

  @Test
  fun `loss drops unreliable messages while reliable ones all arrive in order, later`() {
    val link = link(LinkConditions(latencyTicks = 2, loss = 0.5))
    repeat(MESSAGES) {
      link.clientEnd.sendNumber(ChannelKind.UNRELIABLE, it)
      link.clientEnd.sendNumber(ChannelKind.RELIABLE_ORDERED, it)
      link.clientEnd.sendNumber(ChannelKind.RELIABLE_UNORDERED, it)
    }

    val ordered = link.hostEnd.arrivalsOver(SETTLE_TICKS, ChannelKind.RELIABLE_ORDERED)
    val unordered = link.hostEnd.drain(ChannelKind.RELIABLE_UNORDERED)
    val unreliable = link.hostEnd.drain(ChannelKind.UNRELIABLE)

    assertEquals((0 until MESSAGES).toList(), ordered.flatten())
    assertEquals(emptyList(), ordered.first(), "nothing beats the latency")
    assertTrue(ordered.indexOfLast { it.isNotEmpty() } > 1, "resends arrive later")
    assertEquals((0 until MESSAGES).toList(), unordered.sorted())
    assertTrue(unreliable.size in 1 until MESSAGES, "${unreliable.size} of $MESSAGES arrived")
  }

  @Test
  fun `duplication repeats unreliable messages only`() {
    val link = link(LinkConditions(duplication = 1.0))
    link.clientEnd.sendNumber(ChannelKind.UNRELIABLE, 1)
    link.clientEnd.sendNumber(ChannelKind.RELIABLE_ORDERED, 2)

    assertEquals(listOf(1, 1), link.hostEnd.drain(ChannelKind.UNRELIABLE))
    assertEquals(listOf(2), link.hostEnd.drain(ChannelKind.RELIABLE_ORDERED))
  }

  @Test
  fun `a bandwidth cap lets each tick carry its bytes, in the order sent`() {
    val link = link(LinkConditions(bytesPerTick = 100))
    repeat(6) { link.clientEnd.sendNumber(ChannelKind.UNRELIABLE, it, length = 50) }

    val first = link.hostEnd.drain(ChannelKind.UNRELIABLE)
    val later = link.hostEnd.arrivalsOver(3, ChannelKind.UNRELIABLE)

    assertEquals(listOf(0, 1), first)
    assertEquals(listOf(listOf(2, 3), listOf(4, 5), emptyList()), later)
  }

  @Test
  fun `the same seed gives the same schedule`() {
    fun run(seed: Long): List<List<Int>> {
      val loopback = LoopbackTransport(seed)
      val conditions = LinkConditions(latencyTicks = 1, jitterTicks = 3, loss = 0.3)
      val link = loopback.connect(PeerId(1), conditions)
      repeat(MESSAGES) { link.clientEnd.sendNumber(ChannelKind.UNRELIABLE, it) }
      return List(SETTLE_TICKS) {
        loopback.update()
        link.hostEnd.drain(ChannelKind.UNRELIABLE)
      }
    }

    assertEquals(run(1L), run(1L))
    assertNotEquals(run(1L), run(2L))
  }

  @Test
  fun `conditions change between ticks`() {
    val link = link(LinkConditions(latencyTicks = 5))
    link.conditions = LinkConditions.PERFECT
    link.clientEnd.sendNumber(ChannelKind.UNRELIABLE, 4)

    assertEquals(listOf(4), link.hostEnd.drain(ChannelKind.UNRELIABLE))
  }

  @Test
  fun `closing sends the code to the other end and drops what is in flight`() {
    val link = link(LinkConditions(latencyTicks = 2))
    link.clientEnd.sendNumber(ChannelKind.RELIABLE_ORDERED, 1)
    link.hostEnd.close(CODE)
    transport.update()
    transport.update()

    assertFalse(link.clientEnd.isOpen)
    assertEquals(CODE, link.clientEnd.closeCode)
    assertEquals(emptyList(), link.hostEnd.drain(ChannelKind.RELIABLE_ORDERED))
  }

  private companion object {
    const val MESSAGES = 40
    const val SETTLE_TICKS = 60
    const val CODE = 9
  }
}
