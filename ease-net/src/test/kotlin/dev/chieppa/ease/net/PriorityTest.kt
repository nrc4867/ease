package dev.chieppa.ease.net

import dev.chieppa.ease.ecs.LocalTransform
import dev.chieppa.ease.ecs.replication.replication
import dev.chieppa.ease.net.relevancy.PriorityRule
import dev.chieppa.ease.net.view.ViewSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * When more entities change than a snapshot holds, a nearby fast entity reaches the client every
 * tick, and the distant ones left out rise until they get in, so none falls far behind; that holds
 * too under a rule that scores everything 0, or gives scores that are not numbers.
 */
class PriorityTest {
  private fun overloaded(view: ViewSettings = ViewSettings()): Session {
    val session = Session(entities = FAR + 1, view = view)
    for (index in 1..FAR) session.moveOnHost(index, FAR_X + index)
    session.step()
    return session
  }

  private fun Session.clientY(index: Int): Float = with(client) { mirror(index)[LocalTransform].y }

  private fun Session.moveAll(tick: Int) {
    moveOnHost(0, tick.toFloat())
    for (index in 1..FAR) {
      host.replication.write(held[index], LocalTransform) { y = tick.toFloat() }
    }
  }

  /** Runs [session] with every entity changing each tick; returns the farthest any fell behind. */
  private fun worstLag(session: Session, nearEveryTick: Boolean): Float {
    var worst = 0f
    for (tick in 1..TICKS) {
      session.moveAll(tick)
      session.step()
      if (nearEveryTick) {
        val (hostX, clientX) = session.xs(0)
        assertEquals(hostX, clientX, "the near entity at tick $tick")
      }
      if (tick > WARMUP) {
        for (index in 1..FAR) worst = maxOf(worst, tick - session.clientY(index))
      }
    }
    return worst
  }

  @Test
  fun `a nearby fast entity reaches the client every tick while distant ones wait their turn`() {
    val lag = worstLag(overloaded(), nearEveryTick = true)

    assertTrue(lag in 1f..MAX_LAG, "the farthest behind lagged $lag ticks")
  }

  @Test
  fun `every entity left out converges once changes stop`() {
    val session = overloaded()
    for (tick in 1..WARMUP) {
      session.moveAll(tick)
      session.step()
    }
    repeat(SETTLE) { session.step() }

    for (index in 1..FAR) assertEquals(WARMUP.toFloat(), session.clientY(index), "entity $index")
  }

  @Test
  fun `under a rule scoring everything 0 no entity waits forever`() {
    val lag = worstLag(overloaded(ViewSettings(priority = ZERO)), nearEveryTick = false)

    assertTrue(lag <= MAX_LAG, "the farthest behind lagged $lag ticks")
  }

  @Test
  fun `scores that are not numbers count as 0 and no entity waits forever`() {
    val lag = worstLag(overloaded(ViewSettings(priority = NOT_A_NUMBER)), nearEveryTick = false)

    assertTrue(lag <= MAX_LAG, "the farthest behind lagged $lag ticks")
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

    val ZERO = PriorityRule { _, _, _, _ -> 0f }
    val NOT_A_NUMBER = PriorityRule { _, _, _, _ -> Float.NaN }
  }
}
