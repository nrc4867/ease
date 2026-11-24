package dev.chieppa.ease.viewport.spike

import kotlin.test.Test
import kotlin.test.assertEquals

class FrameRateTest {
  private var clock = 0L
  private val rate = FrameRate { clock }

  @Test
  fun `counts the frames of the last whole second`() {
    repeat(60) {
      clock += 1_000_000_000L / 60
      rate.tick()
    }
    clock += 1_000_000_000L / 60
    rate.tick()

    assertEquals(60f, rate.perSecond, 1f)
  }

  @Test
  fun `shows nothing before a second has passed`() {
    clock += 500_000_000L
    rate.tick()

    assertEquals(0f, rate.perSecond)
  }
}
