package dev.chieppa.ease.net

import dev.chieppa.ease.net.channel.ChannelKind
import dev.chieppa.ease.net.channel.Channels
import dev.chieppa.ease.net.command.CommandExecutor
import dev.chieppa.ease.net.command.RateLimit
import dev.chieppa.ease.net.command.RateLimits
import dev.chieppa.ease.net.session.NetSettings
import dev.chieppa.ease.testing.AllocationBudget
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * A session's hot paths allocate nothing once warm: building, sending and applying snapshots, and
 * sending and taking input. Commands allocate only what their own objects need: the boxes the
 * serialization plugin makes around a `NetworkId` field, the command decoded, and its targets.
 */
class NetAllocationTest {
  private val unlimited = RateLimit(commands = Int.MAX_VALUE / 2, burst = Int.MAX_VALUE / 2)
  private val settings = NetSettings(rateLimits = RateLimits(unlimited, unlimited))
  private var executed = 0
  private val session =
    Session(ENTITIES, settings = settings, executor = CommandExecutor { _, _, _ -> executed++ })
  private val scratch = ByteArray(Channels.MAX_RELIABLE_BYTES)

  @Test
  fun `a tick of a session with every entity moving allocates nothing`() {
    var x = 0f
    val tick = Runnable {
      x += 1f
      for (index in 0 until ENTITIES) session.moveOnHost(index, x + index)
      session.step()
    }

    AllocationBudget.assertWithin(0, tick, WARMUP, MEASURED)
  }

  @Test
  fun `a quiet tick allocates nothing`() {
    AllocationBudget.assertWithin(0, { session.step() }, WARMUP, MEASURED)
  }

  @Test
  fun `sending a command allocates at most the box around its network id`() {
    val push = Push(session.idOf(0), 0.5f)
    val send = Runnable {
      session.clientSession.sendCommand(session.tick, push)
      session.link.hostEnd.receive(ChannelKind.RELIABLE_ORDERED, scratch)
    }

    AllocationBudget.assertWithin(BOX_BYTES, send, WARMUP, MEASURED)
  }

  @Test
  fun `checking a received command allocates only the command's own objects`() {
    val push = Push(session.idOf(0), 0.5f)
    val receive = Runnable {
      session.clientSession.sendCommand(session.tick, push)
      session.hostSession.receive(session.tick)
    }

    AllocationBudget.assertWithin(PUSH_BYTES, receive, WARMUP, MEASURED)
    assertTrue(executed > 0)
  }

  private companion object {
    const val ENTITIES = 64
    const val WARMUP = 2_000
    const val MEASURED = 2_000

    /** A boxed `NetworkId`: an object header and a long. */
    const val BOX_BYTES = 24L

    /**
     * Sending and receiving a Push: the box sending makes (24 bytes), the Push (24), the box its
     * generated deserializer reads through (24), and the list `targets()` returns with its own box
     * (40).
     */
    const val PUSH_BYTES = 112L
  }
}
