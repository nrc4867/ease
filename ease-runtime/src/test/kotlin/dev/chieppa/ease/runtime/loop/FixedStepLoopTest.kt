package dev.chieppa.ease.runtime.loop

import dev.chieppa.ease.platform.time.ManualClock
import dev.chieppa.ease.testing.AllocationBudget
import kotlin.test.Test
import kotlin.test.assertEquals

class FixedStepLoopTest {
  private val step = FixedStep(stepNanos = 1_000, maxStepsPerFrame = 5)
  private val clock = ManualClock()
  private val ticks = mutableListOf<Long>()
  private val loop = FixedStepLoop(clock, step) { ticks += it }

  @Test
  fun `the first frame starts the clock and runs no step`() {
    clock.advance(50_000)

    assertEquals(0, loop.frame())
    assertEquals(emptyList(), ticks)
  }

  @Test
  fun `each step's worth of time runs one step, numbered from 0`() {
    loop.frame()
    repeat(3) {
      clock.advance(1_000)
      loop.frame()
    }

    assertEquals(listOf(0L, 1L, 2L), ticks)
    assertEquals(3, loop.tick)
    assertEquals(0f, loop.alpha)
  }

  @Test
  fun `a partial step waits and sets alpha`() {
    loop.frame()
    clock.advance(2_500)

    assertEquals(2, loop.frame())
    assertEquals(0.5f, loop.alpha)
  }

  @Test
  fun `partial steps add up across frames`() {
    loop.frame()
    clock.advance(600)
    assertEquals(0, loop.frame())
    clock.advance(600)

    assertEquals(1, loop.frame())
    assertEquals(0.2f, loop.alpha, 1e-6f)
  }

  @Test
  fun `a slow frame runs at most the cap and drops the backlog`() {
    loop.frame()
    clock.advance(100_250)

    assertEquals(5, loop.frame())
    assertEquals(95_000, loop.droppedNanos)
    assertEquals(0.25f, loop.alpha)
  }

  @Test
  fun `the loop holds its step after a slow frame`() {
    loop.frame()
    clock.advance(100_000)
    loop.frame()
    clock.advance(1_000)

    assertEquals(1, loop.frame())
    assertEquals(6, loop.tick)
  }

  @Test
  fun `a step on demand needs no clock`() {
    loop.stepOnce()
    loop.stepOnce()

    assertEquals(listOf(0L, 1L), ticks)
  }

  @Test
  fun `a frame allocates nothing`() {
    val quiet = FixedStepLoop(clock, step) {}
    quiet.frame()

    AllocationBudget.assertNoAllocation {
      clock.advance(1_500)
      quiet.frame()
    }
  }
}
