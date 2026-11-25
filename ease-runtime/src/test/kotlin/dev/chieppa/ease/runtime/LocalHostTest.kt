package dev.chieppa.ease.runtime

import dev.chieppa.ease.ecs.LocalTransform
import dev.chieppa.ease.ecs.replication.PeerId
import dev.chieppa.ease.net.transport.LinkConditions
import dev.chieppa.ease.runtime.loop.FixedStep
import dev.chieppa.ease.testing.AllocationBudget
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * A game as the player runs it: a host world and a local client world joined over the loopback
 * transport. The host makes everything, the client receives it, and input flows back.
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
    assertEquals(x(local.host), x(local.client.world))
  }

  @Test
  fun `the game welcomes the local client once, as it would any peer`() {
    repeat(STEPS) { local.step() }

    assertEquals(listOf(LocalHost.LOCAL_PEER), joins)
  }

  @Test
  fun `the client's input reaches the host every tick, with no delay by default`() {
    repeat(STEPS) { local.step() }

    val peer = local.session.peers.single()
    assertEquals(STEPS - 1L, peer.inputs.lastProcessedTick)
    assertEquals(STEPS - 1L, local.client.session.lastProcessedInputTick)
  }

  @Test
  fun `under simulated latency the client lags the host by the round trip`() {
    local.link.conditions = LinkConditions(latencyTicks = LATENCY)
    repeat(STEPS) { local.step() }

    assertEquals(STEPS.toFloat(), x(local.host))
    assertEquals(STEPS - LATENCY.toFloat(), x(local.client.world))
  }

  @Test
  fun `a tick of host and client over the loopback allocates nothing`() {
    AllocationBudget.assertWithin(0, { local.step() }, WARMUP, MEASURED)
  }

  private companion object {
    const val STEPS = 10
    const val LATENCY = 2
    const val WARMUP = 5_000
    const val MEASURED = 1_000
  }
}
