package dev.chieppa.ease.platform.time

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ManualClockTest {
  @Test
  fun `the clock moves only when advanced`() {
    val clock = ManualClock(start = 100)

    assertEquals(100, clock.nanoTime())
    clock.advance(25)
    clock.advance(0)

    assertEquals(125, clock.nanoTime())
  }

  @Test
  fun `the clock cannot move back`() {
    assertFailsWith<IllegalArgumentException> { ManualClock().advance(-1) }
  }
}
