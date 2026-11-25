package dev.chieppa.ease.net

import dev.chieppa.ease.ecs.LocalTransform
import dev.chieppa.ease.ecs.replication.replication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * When more entities change than a snapshot holds, a nearby fast entity reaches the client every
 * tick, and the distant ones left out rise until they get in, so none falls far behind.
 */
class PriorityTest {
  private val session = Session(entities = FAR + 1)

  init {
    for (index in 1..FAR) session.moveOnHost(index, FAR_X + index)
    session.step()
  }

  private fun clientY(index: Int): Float =
    with(session.client) { session.mirror(index)[LocalTransform].y }

  private fun moveAll(tick: Int) {
    session.moveOnHost(0, tick.toFloat())
    for (index in 1..FAR) {
      session.host.replication.write(session.held[index], LocalTransform) { y = tick.toFloat() }
    }
  }

  @Test
  fun `a nearby fast entity reaches the client every tick while distant ones wait their turn`() {
    var worstLag = 0f
    for (tick in 1..TICKS) {
      moveAll(tick)
      session.step()
      val (hostX, clientX) = session.xs(0)
      assertEquals(hostX, clientX, "the near entity at tick $tick")
      if (tick > WARMUP) {
        for (index in 1..FAR) worstLag = maxOf(worstLag, tick - clientY(index))
      }
    }

    assertTrue(worstLag in 1f..MAX_LAG, "the farthest behind lagged $worstLag ticks")
  }

  @Test
  fun `every entity left out converges once changes stop`() {
    for (tick in 1..WARMUP) {
      moveAll(tick)
      session.step()
    }
    repeat(SETTLE) { session.step() }

    for (index in 1..FAR) assertEquals(WARMUP.toFloat(), clientY(index), "entity $index")
  }

  private companion object {
    /** More distant entities than one snapshot carries changes for. */
    const val FAR = 400
    const val FAR_X = 100f
    const val TICKS = 40
    const val WARMUP = 10
    const val SETTLE = 10

    /** A distant entity is sent at least this often, in ticks. */
    const val MAX_LAG = 6f
  }
}
