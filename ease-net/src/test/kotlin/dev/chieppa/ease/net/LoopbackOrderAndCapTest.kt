package dev.chieppa.ease.net

import dev.chieppa.ease.ecs.replication.PeerId
import dev.chieppa.ease.net.channel.ChannelKind
import dev.chieppa.ease.net.channel.Channels
import dev.chieppa.ease.net.channel.Connection
import dev.chieppa.ease.net.transport.LinkConditions
import dev.chieppa.ease.net.transport.LoopbackTransport
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The ordered channel keeps its order whatever the simulator draws, across a seeded sweep of
 * schedules mixing resends, jitter and a tight bandwidth cap; and the cap charges every byte that
 * leaves, duplicates included, without refilling when the conditions change mid-tick.
 */
class LoopbackOrderAndCapTest {
  private val target = ByteArray(Channels.MAX_RELIABLE_BYTES)

  /** Sends ordered messages of seeded sizes for a while, then returns what arrived, in order. */
  private fun orderedRun(seed: Int, conditions: LinkConditions): List<Int> {
    val transport = LoopbackTransport(seed.toLong())
    val link = transport.connect(PeerId(1), conditions)
    val sizes = Random(seed)
    val arrived = mutableListOf<Int>()
    var sent = 0
    repeat(SENDING_TICKS + SETTLE_TICKS) { tick ->
      if (tick < SENDING_TICKS) {
        repeat(sizes.nextInt(0, MAX_PER_TICK + 1)) {
          link.clientEnd.sendSequence(sent++, sizes.nextInt(2, MAX_BYTES))
        }
      }
      transport.update()
      arrived += link.hostEnd.drainSequences()
    }
    assertEquals(sent, arrived.size, "seed $seed: every message arrives")
    return arrived
  }

  private fun Connection.sendSequence(number: Int, length: Int) {
    val bytes = ByteArray(length)
    bytes[0] = (number and BYTE).toByte()
    bytes[1] = (number ushr Byte.SIZE_BITS).toByte()
    send(ChannelKind.RELIABLE_ORDERED, bytes, 0, length)
  }

  private fun Connection.drainSequences(): List<Int> = buildList {
    while (true) {
      if (receive(ChannelKind.RELIABLE_ORDERED, target) == Channels.NO_MESSAGE) break
      add((target[0].toInt() and BYTE) or ((target[1].toInt() and BYTE) shl Byte.SIZE_BITS))
    }
  }

  private fun Connection.sendBytes(kind: ChannelKind, length: Int) {
    send(kind, ByteArray(length), 0, length)
  }

  private fun Connection.count(kind: ChannelKind): Int {
    var count = 0
    while (receive(kind, target) != Channels.NO_MESSAGE) count++
    return count
  }

  @Test
  fun `the ordered channel keeps its order under resends, jitter and a tight cap`() {
    val schedules =
      listOf(
        LinkConditions(loss = 0.3, bytesPerTick = 60),
        LinkConditions(jitterTicks = 3),
        LinkConditions(jitterTicks = 2, loss = 0.3, bytesPerTick = 100),
        LinkConditions(latencyTicks = 1, jitterTicks = 2, loss = 0.3, bytesPerTick = 60),
      )
    for (conditions in schedules) {
      for (seed in 0 until SEEDS) {
        val arrived = orderedRun(seed, conditions)
        assertEquals(arrived.sorted(), arrived, "seed $seed arrived out of order")
      }
    }
  }

  @Test
  fun `a duplicated message spends the cap for both copies`() {
    val transport = LoopbackTransport()
    val link = transport.connect(PeerId(1), LinkConditions(duplication = 1.0, bytesPerTick = 100))
    link.clientEnd.sendBytes(ChannelKind.UNRELIABLE, 50)
    link.clientEnd.sendBytes(ChannelKind.UNRELIABLE, 50)

    assertEquals(2, link.hostEnd.count(ChannelKind.UNRELIABLE), "the first and its copy")
    transport.update()
    assertEquals(2, link.hostEnd.count(ChannelKind.UNRELIABLE), "the second waited a tick")
  }

  @Test
  fun `changing conditions mid-tick clamps the budget without refilling it`() {
    val transport = LoopbackTransport()
    val link = transport.connect(PeerId(1), LinkConditions(bytesPerTick = 100))
    link.clientEnd.sendBytes(ChannelKind.UNRELIABLE, 100)
    link.conditions = LinkConditions(bytesPerTick = 100)
    link.clientEnd.sendBytes(ChannelKind.UNRELIABLE, 100)

    assertEquals(1, link.hostEnd.count(ChannelKind.UNRELIABLE))
    transport.update()
    assertEquals(1, link.hostEnd.count(ChannelKind.UNRELIABLE))
  }

  private companion object {
    const val SEEDS = 48
    const val SENDING_TICKS = 30
    const val SETTLE_TICKS = 300
    const val MAX_PER_TICK = 3
    const val MAX_BYTES = 80
    const val BYTE = 0xFF
  }
}
