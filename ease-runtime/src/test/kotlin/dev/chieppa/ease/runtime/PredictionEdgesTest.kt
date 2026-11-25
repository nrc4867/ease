package dev.chieppa.ease.runtime

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.World.Companion.family
import dev.chieppa.ease.commands.builtin.Destroy
import dev.chieppa.ease.ecs.LocalTransform
import dev.chieppa.ease.ecs.replication.Authority
import dev.chieppa.ease.ecs.replication.PeerId
import dev.chieppa.ease.ecs.replication.networkIdOf
import dev.chieppa.ease.net.prediction.PredictionSettings
import dev.chieppa.ease.net.session.CloseReason
import dev.chieppa.ease.net.transport.LinkConditions
import dev.chieppa.ease.runtime.loop.FixedStep
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * Prediction at its edges: a latency spike that comes and goes, a snapshot too full to carry the
 * walker, a lead past the kept history, a peer that leaves while moving, and a corrected walker
 * that despawns.
 */
class PredictionEdgesTest {
  private fun walker(world: HostedWorld): Entity =
    with(world.world) {
      family { all(LocalTransform, Authority) }.first { it[Authority].owner != PeerId.HOST }
    }

  private fun x(world: HostedWorld): Float = with(world.world) { walker(world)[LocalTransform].x }

  /** Steps [local] with the walker's input at [axis] and returns how many ticks its client ran. */
  private fun step(local: LocalHost, axis: Float): Long {
    local.client.inputFrame.setAxis(0, axis)
    val before = local.client.tick
    local.step()
    return local.client.tick - before
  }

  /** Steps [steps] times, checking the walker moves exactly as far as its client's ticks say. */
  private fun assertWalksSteadily(local: LocalHost, steps: Int) {
    repeat(steps) {
      val before = x(local.client.world)
      val ran = step(local, 1f)
      val at = local.tick
      assertEquals(before + ran * WalkModule.STEP, x(local.client.world), TOLERANCE, "at $at")
    }
  }

  @Test
  fun `after a latency spike clears, the client gives back its lead and its input arrives again`() {
    val local = LocalHost(WalkModule(), FixedStep.perSecond())
    local.link.conditions = LinkConditions(latencyTicks = SPIKE)
    repeat(SETTLE) { step(local, 0f) }
    local.link.conditions = LinkConditions(latencyTicks = CALM)
    repeat(SETTLE) { step(local, 0f) }
    val peer = local.session.peers.single()
    val dropped = peer.inputs.droppedPackets

    repeat(SETTLE) {
      step(local, 0f)
      assertEquals(local.tick - 1, peer.inputs.lastProcessedTick, "input at host ${local.tick}")
    }
    assertEquals(dropped, peer.inputs.droppedPackets, "no input fell outside the window")
  }

  @Test
  fun `a walker a full snapshot leaves out is not rewound to its older state`() {
    val local = LocalHost(WalkModule(crowd = CROWD), FixedStep.perSecond())
    local.link.conditions = LinkConditions(latencyTicks = CALM)
    repeat(SETTLE) { step(local, 1f) }
    val replays = local.client.replays

    assertWalksSteadily(local, SETTLE)

    assertEquals(replays, local.client.replays, "nothing disagreed, so nothing replayed")
  }

  @Test
  fun `a lead past the kept history keeps the prediction instead of a stale host state`() {
    val prediction = PredictionSettings(historyTicks = SHORT_HISTORY)
    val local = LocalHost(WalkModule(), FixedStep.perSecond(), prediction = prediction)
    local.link.conditions = LinkConditions(latencyTicks = LONG_LATENCY)
    repeat(SETTLE) { step(local, 1f) }

    assertTrue(local.client.tick - local.client.session.latestSnapshotTick > SHORT_HISTORY)
    repeat(SETTLE) {
      val before = x(local.client.world)
      val ran = step(local, 1f)
      assertEquals(before + ran * WalkModule.STEP, x(local.client.world), TOLERANCE)
      assertTrue(x(local.client.world) > x(local.host), "the client is ahead of the host")
    }
  }

  @Test
  fun `a peer whose connection closes stops moving on the host`() {
    val local = LocalHost(WalkModule(), FixedStep.perSecond())
    repeat(SETTLE) { step(local, 1f) }
    val clientWorld = local.client.world
    val id = clientWorld.world.networkIdOf(walker(clientWorld))

    repeat(FLOOD) { local.client.send(Destroy(id)) }
    repeat(CALM) { step(local, 1f) }
    val peer = local.session.peers.single()
    assertEquals(CloseReason.RATE_LIMITED, peer.closeReason)
    val stopped = x(local.host)
    repeat(SETTLE) { step(local, 1f) }

    assertEquals(stopped, x(local.host))
  }

  @Test
  fun `a corrected walker that despawns leaves no offset behind for its entity id`() {
    var gusting = true
    val local = LocalHost(WalkModule(gusts = { gusting }), FixedStep.perSecond())
    val client = local.client
    repeat(SETTLE) { step(local, 1f) }
    val walker = walker(client.world)
    while (client.world.corrections.y(walker.id) == 0f) step(local, 1f)
    gusting = false

    local.host.execute(Destroy(local.host.world.networkIdOf(walker(local.host))))
    repeat(CALM) { step(local, 1f) }

    assertNotEquals(0, local.host.world.numEntities)
    assertEquals(0f, client.world.corrections.y(walker.id))
    assertEquals(0f, client.world.corrections.x(walker.id))
  }

  private companion object {
    const val SPIKE = 40
    const val CALM = 2
    const val SETTLE = 150
    const val CROWD = 300
    const val SHORT_HISTORY = 16
    const val LONG_LATENCY = 12
    const val FLOOD = 400
    const val TOLERANCE = 1e-3f
  }
}
