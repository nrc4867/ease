package dev.chieppa.ease.net

import dev.chieppa.ease.net.transport.LinkConditions
import dev.chieppa.ease.testing.AllocationBudget
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * A session over the loopback transport on a bad simulated network: the client still converges to
 * the host once the host stops changing, and a session tick still allocates nothing.
 */
class SimulatedSessionTest {
  private val session = Session(ENTITIES)

  init {
    session.link.conditions = BAD
  }

  private fun moveFor(ticks: Int) {
    repeat(ticks) { tick ->
      for (index in 0 until ENTITIES) session.moveOnHost(index, tick + index.toFloat())
      session.step()
    }
  }

  @Test
  fun `the client converges to the host through latency, jitter, loss, duplication and a cap`() {
    moveFor(MOVING_TICKS)
    repeat(QUIET_TICKS) { session.step() }

    val (host, client) = session.states()
    assertEquals(host, client)
    assertTrue(session.clientSession.isOpen)
  }

  @Test
  fun `a session tick on a bad network allocates nothing`() {
    moveFor(MOVING_TICKS)
    var x = 0f
    val tick = Runnable {
      x += 1f
      for (index in 0 until ENTITIES) session.moveOnHost(index, x + index)
      session.step()
    }

    AllocationBudget.assertWithin(0, tick, WARMUP, MEASURED)
  }

  private companion object {
    const val ENTITIES = 8
    const val MOVING_TICKS = 120
    const val QUIET_TICKS = 90
    const val WARMUP = 2_000
    const val MEASURED = 2_000

    val BAD =
      LinkConditions(
        latencyTicks = 3,
        jitterTicks = 2,
        loss = 0.2,
        duplication = 0.1,
        bytesPerTick = 2_000,
      )
  }
}
