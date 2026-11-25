package dev.chieppa.ease.runtime

import com.github.quillraven.fleks.World.Companion.family
import dev.chieppa.ease.ecs.LocalTransform
import dev.chieppa.ease.ecs.replication.Authority
import dev.chieppa.ease.ecs.replication.PeerId
import dev.chieppa.ease.net.prediction.ClockSettings
import dev.chieppa.ease.net.prediction.InterpolationClock
import dev.chieppa.ease.net.transport.LinkConditions
import dev.chieppa.ease.runtime.loop.FixedStep
import dev.chieppa.ease.testing.AllocationBudget
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Under a simulated network with latency, jitter and loss: the host's drifter moves smoothly on the
 * client, drawn a little in the past; the client's clock keeps its input arriving in time; and a
 * tick of all of it, prediction, replays and interpolation included, allocates nothing.
 */
class RemoteAndClockTest {
  private val local = LocalHost(WalkModule(gusts = { true }), FixedStep.perSecond())
  private val client = local.client

  private fun drifterX(world: HostedWorld): Float =
    with(world.world) {
      val drifter = family {
        all(LocalTransform, Authority)
      }
        .first {
          it[Authority].owner == PeerId.HOST
        }
      drifter[LocalTransform].x
    }

  @Test
  fun `the host's drifter moves smoothly on the client, a few ticks in the past`() {
    local.link.conditions = LinkConditions(latencyTicks = 4, jitterTicks = 3, loss = 0.2)
    repeat(WARMUP) { local.step() }
    var last = drifterX(client.world)

    repeat(RUN) {
      local.step()
      val now = drifterX(client.world)
      val moved = now - last
      assertTrue(moved in SMOOTH, "the drifter moved $moved in one step at host tick ${local.tick}")
      last = now
      val behind = drifterX(local.host) - now
      assertTrue(behind in BEHIND, "the client drew the drifter $behind behind the host")
    }
  }

  @Test
  fun `the client's clock keeps its input arriving before the host needs it`() {
    val latency = 5
    local.link.conditions = LinkConditions(latencyTicks = latency, jitterTicks = 2)
    repeat(WARMUP) { local.step() }
    val peer = local.session.peers.single()

    repeat(RUN) {
      local.step()
      assertEquals(local.host.tick - 1, peer.inputs.lastProcessedTick, "input at ${local.tick}")
    }
    val lead = client.tick - local.host.tick
    val least = latency + ClockSettings.DEFAULT_BUFFER_TICKS.toLong()
    assertTrue(lead in least..least + JITTER_ROOM, "the client leads the host by $lead ticks")
  }

  @Test
  fun `a tick under latency, jitter and loss, with prediction and replays, allocates nothing`() {
    local.link.conditions = LinkConditions(latencyTicks = 3, jitterTicks = 2, loss = 0.1)
    var tick = 0L
    val walk = { client.inputFrame.setAxis(0, if (tick++ / TURN % 2 == 0L) 1f else -1f) }

    AllocationBudget.assertWithin(0, { walk().also { local.step() } }, ALLOCATION_WARMUP, MEASURED)

    assertTrue(client.replays > 0, "the measured ticks replayed corrections")
  }

  private companion object {
    const val WARMUP = 120
    const val RUN = 300
    const val TURN = 40L
    const val JITTER_ROOM = 4L
    const val ALLOCATION_WARMUP = 5_000
    const val MEASURED = 1_000

    /** A tick moves the drifter one unit; the clock may run a little fast or slow. */
    val SMOOTH = 0.5f..1.5f

    /** The delay plus the snapshot's trip, give or take the jitter. */
    const val MOST_BEHIND = 18f
    val BEHIND = InterpolationClock.DEFAULT_DELAY_TICKS.toFloat()..MOST_BEHIND
  }
}
