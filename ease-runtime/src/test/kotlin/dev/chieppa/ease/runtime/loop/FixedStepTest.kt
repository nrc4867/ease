package dev.chieppa.ease.runtime.loop

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class FixedStepTest {
  @Test
  fun `a rate per second sets the step length`() {
    val step = FixedStep.perSecond(stepsPerSecond = 50, maxStepsPerFrame = 4)

    assertEquals(20_000_000, step.stepNanos)
    assertEquals(4, step.maxStepsPerFrame)
  }

  @Test
  fun `empty steps and caps are rejected`() {
    assertFailsWith<IllegalArgumentException> { FixedStep(stepNanos = 0, maxStepsPerFrame = 1) }
    assertFailsWith<IllegalArgumentException> { FixedStep(stepNanos = 1, maxStepsPerFrame = 0) }
    assertFailsWith<IllegalArgumentException> { FixedStep.perSecond(stepsPerSecond = 0) }
  }
}
