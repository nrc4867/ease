package dev.chieppa.ease.net

import com.github.quillraven.fleks.Entity
import dev.chieppa.ease.net.channel.ChannelKind
import dev.chieppa.ease.net.channel.Channels
import dev.chieppa.ease.net.command.CommandExecutor
import dev.chieppa.ease.net.command.RateLimit
import dev.chieppa.ease.net.command.RateLimits
import dev.chieppa.ease.net.relevancy.AlwaysRelevantFilter
import dev.chieppa.ease.net.relevancy.AreaFilter
import dev.chieppa.ease.net.relevancy.DistanceFilter
import dev.chieppa.ease.net.relevancy.Team
import dev.chieppa.ease.net.relevancy.TeamFilter
import dev.chieppa.ease.net.session.NetSettings
import dev.chieppa.ease.net.view.ViewSettings
import dev.chieppa.ease.testing.AllocationBudget
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * A session's hot paths allocate nothing once warm: relevancy, priority, building, sending and
 * applying snapshots, and sending and taking input. The session runs every built-in filter, with a
 * quarter of its entities on a team the client is not on, so they stay out of its view. Commands
 * allocate only what their own objects need: the boxes the serialization plugin makes around a
 * `NetworkId` field, the command decoded, and its targets.
 */
class NetAllocationTest {
  private val unlimited = RateLimit(commands = Int.MAX_VALUE / 2, burst = Int.MAX_VALUE / 2)
  private val settings = NetSettings(rateLimits = RateLimits(unlimited, unlimited))
  private var executed = 0
  private val view =
    ViewSettings(listOf(DistanceFilter(RADIUS), TeamFilter, AreaFilter, AlwaysRelevantFilter))
  private val session =
    Session(
      ENTITIES,
      owned = ENTITIES / 2,
      settings = settings,
      executor = CommandExecutor { _, _, _ -> executed++ },
      view = view,
    )

  init {
    for (index in ENTITIES * 3 / 4 until ENTITIES) {
      with(session.host) { session.held[index].configure { it += Team(OTHER_TEAM) } }
    }
    session.step()
  }

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
  fun `entities out of view stay out, and their changes are never sent`() {
    session.moveOnHost(ENTITIES - 1, 1f)
    session.step()

    assertEquals(Entity.NONE, session.mirror(ENTITIES - 1))
    assertEquals(0f to 0f, session.xs(0))
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
    const val RADIUS = 1e9f
    const val OTHER_TEAM = 9
    const val WARMUP = 2_000
    const val MEASURED = 2_000

    /** A boxed `NetworkId`: an object header and a long. */
    const val BOX_BYTES = 24L

    /**
     * Sending and receiving a Push: the Push (24 bytes), up to three network id boxes (the one
     * sending makes, the one its generated deserializer reads through, and the one in the list
     * `targets()` returns, 24 each) and that list (16). The JIT removes some boxes on some
     * machines: 96 bytes on Windows, 120 on macOS; the budget allows for object layouts to differ.
     */
    const val PUSH_BYTES = 128L
  }
}
