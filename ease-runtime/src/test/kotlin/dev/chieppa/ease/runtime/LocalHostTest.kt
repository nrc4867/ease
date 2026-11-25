package dev.chieppa.ease.runtime

import dev.chieppa.ease.ecs.LocalTransform
import dev.chieppa.ease.ecs.Phase
import dev.chieppa.ease.ecs.PhasedSystems
import dev.chieppa.ease.ecs.replication.PeerId
import dev.chieppa.ease.net.prediction.ClockSettings
import dev.chieppa.ease.net.transport.LinkConditions
import dev.chieppa.ease.runtime.loop.FixedStep
import dev.chieppa.ease.testing.AllocationBudget
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * A game as the player runs it: a host world and a local client world joined over the loopback
 * transport. The host makes everything, the client receives it and draws it a few ticks in the
 * past, and input flows back ahead of the host's tick.
 */
class LocalHostTest {
  private val joins = mutableListOf<PeerId>()
  private val module =
    object : GameModule by DriftModule() {
      override fun peerJoined(world: HostedWorld, peer: PeerId) {
        joins += peer
      }
    }
  private val local = LocalHost(module, FixedStep.perSecond())

  private fun x(world: HostedWorld): Float =
    with(world.world) { asEntityBag().single()[LocalTransform].x }

  @Test
  fun `only the host populates, and the client receives the host's entities and follows them`() {
    val client = local.client.world
    assertEquals(0, client.world.numEntities)

    repeat(STEPS) { local.step() }

    val hostLog = local.host.history.log
    assertEquals(1, hostLog.all.size)
    val clientLog = client.history.log
    assertTrue(clientLog.all.isEmpty())
    assertEquals(STEPS.toFloat(), x(local.host))
    val shown = local.client.interpolationTime.toFloat() + 1f
    assertEquals(shown, x(local.client.world), TOLERANCE)
    assertTrue(x(local.client.world) < x(local.host), "the client draws the host's past")
  }

  @Test
  fun `the game welcomes the local client once, as it would any peer`() {
    repeat(STEPS) { local.step() }

    assertEquals(listOf(LocalHost.LOCAL_PEER), joins)
  }

  @Test
  fun `the client's input reaches the host every tick, ahead of it by the clock's buffer`() {
    repeat(STEPS) { local.step() }

    val peer = local.session.peers.single()
    assertEquals(STEPS - 1L, peer.inputs.lastProcessedTick)
    assertEquals(STEPS - 2L, local.client.session.lastProcessedInputTick)
    assertEquals(STEPS + ClockSettings.DEFAULT_BUFFER_TICKS.toLong(), local.client.tick)
  }

  @Test
  fun `under simulated latency the client leads the host by the latency and its buffer`() {
    local.link.conditions = LinkConditions(latencyTicks = LATENCY)
    repeat(STEPS * STEPS) { local.step() }

    val lead = local.client.tick - local.host.tick
    assertEquals(LATENCY + ClockSettings.DEFAULT_BUFFER_TICKS.toLong(), lead)
    val peer = local.session.peers.single()
    assertEquals(local.host.tick - 1, peer.inputs.lastProcessedTick)
  }

  @Test
  fun `host systems run only in the host's world, shared systems in every world`() {
    val ticks = mutableMapOf<PeerId, Int>()
    val counting =
      object : GameModule by DriftModule() {
        override fun systems(systems: PhasedSystems) {
          systems.add(Phase.INPUT, TickCounter(ticks))
        }

        override fun hostSystems(systems: PhasedSystems) {
          DriftModule().hostSystems(systems)
        }
      }
    val pair = LocalHost(counting, FixedStep.perSecond())
    val second = pair.addClient(PeerId(2))

    repeat(STEPS) { pair.step() }

    assertEquals(
      mapOf(PeerId.HOST to STEPS, LocalHost.LOCAL_PEER to STEPS, PeerId(2) to STEPS),
      ticks,
    )
    assertEquals(STEPS.toFloat(), x(pair.host))
    assertEquals(second.interpolationTime.toFloat() + 1f, x(second.world), TOLERANCE)
  }

  @Test
  fun `a tick of host and client over the loopback allocates nothing`() {
    AllocationBudget.assertWithin(0, { local.step() }, WARMUP, MEASURED)
  }

  private companion object {
    const val STEPS = 10
    const val LATENCY = 2
    const val TOLERANCE = 1e-3f
    const val WARMUP = 5_000
    const val MEASURED = 1_000
  }
}
