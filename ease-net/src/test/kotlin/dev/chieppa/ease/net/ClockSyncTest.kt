package dev.chieppa.ease.net

import dev.chieppa.ease.net.input.InputFrame
import dev.chieppa.ease.net.input.InputLayout
import dev.chieppa.ease.net.input.InputRing
import dev.chieppa.ease.net.prediction.ClockSettings
import dev.chieppa.ease.net.prediction.ClockSync
import dev.chieppa.ease.net.prediction.InterpolationClock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

/** A client's clock, its interpolation time and its kept input, fed by hand. */
class ClockSyncTest {
  private val settings = ClockSettings()
  private val buffer = settings.bufferTicks.toLong()

  @Test
  fun `the first report jumps the client ahead to its buffer, later ones steer the rate`() {
    val clock = ClockSync(settings)

    assertEquals(buffer + LATE, clock.observe(-LATE, inputTick = 10, clientTick = 11))
    assertEquals(1.0, clock.rate)

    assertEquals(0L, clock.observe(buffer - 1, inputTick = 20, clientTick = 21))
    assertTrue(clock.rate > 1.0, "a little late: faster, at ${clock.rate}")
    repeat(settings.window) { clock.observe(buffer + 2, inputTick = 30L + it, clientTick = 40) }
    assertTrue(clock.rate < 1.0, "early: slower, at ${clock.rate}")
    assertTrue(clock.rate >= 1.0 - settings.maxAdjust)
  }

  @Test
  fun `reports on input sent before a jump are ignored`() {
    val clock = ClockSync(settings)
    clock.observe(-LATE, inputTick = 10, clientTick = 11)

    assertEquals(0L, clock.observe(-LATE, inputTick = 12, clientTick = 16))

    assertEquals(1.0, clock.rate)
  }

  @Test
  fun `a client far behind jumps again rather than crawl back`() {
    val clock = ClockSync(settings)
    clock.observe(buffer, inputTick = 1, clientTick = 2)

    val jump = clock.observe(-settings.resyncTicks.toLong(), inputTick = 5, clientTick = 6)

    assertEquals(buffer + settings.resyncTicks, jump)
  }

  @Test
  fun `a client far ahead holds still until the host has caught up, never stepping back`() {
    val clock = ClockSync(settings)
    clock.observe(buffer, inputTick = 1, clientTick = 2)

    val jump = clock.observe(buffer + AHEAD, inputTick = 5, clientTick = 6)
    val held = (0 until AHEAD).map { clock.ticksThisStep() }

    assertEquals(0L, jump)
    assertTrue(held.all { it == 0 }, "held for $AHEAD host ticks: $held")
    assertEquals(1, clock.ticksThisStep())
    assertEquals(0L, clock.observe(buffer + AHEAD, inputTick = 5, clientTick = 6), "stale report")
  }

  @Test
  fun `the rate runs extra or fewer ticks over many host ticks, never skipping one`() {
    val clock = ClockSync(settings)
    clock.observe(buffer, inputTick = 1, clientTick = 2)
    repeat(settings.window) { clock.observe(buffer - 2, inputTick = 2L + it, clientTick = 3) }

    val ran = (0 until HOST_TICKS).sumOf { clock.ticksThisStep() }

    assertTrue(ran in HOST_TICKS + 1..HOST_TICKS * 2, "ran $ran ticks for $HOST_TICKS")
  }

  @Test
  fun `interpolation time trails the newest snapshot by its delay and moves a tick a tick`() {
    val clock = InterpolationClock(delayTicks = DELAY)
    clock.advance(latestSnapshotTick = 100)
    assertEquals(100.0 - DELAY, clock.time)

    clock.advance(latestSnapshotTick = 101)

    assertEquals(101.0 - DELAY, clock.time, 1e-9)
  }

  @Test
  fun `kept input replays a skipped tick with the frame before it`() {
    val layout = InputLayout(axes = 1, buttons = 0)
    val ring = InputRing(layout, capacity = 8)
    val frame = InputFrame(layout).also { it.setAxis(0, 1f) }
    ring.record(3, frame)
    frame.setAxis(0, -1f)
    ring.record(6, frame)

    assertEquals(1f, ring.frameAt(4).axis(0))
    assertEquals(-1f, ring.frameAt(6).axis(0))
    assertSame(ring.frameAt(2), ring.frameAt(1), "no input before the first kept tick")
    assertTrue(!ring.reaches(-3), "the ring reaches only its last 8 ticks")
    assertTrue(!ring.reaches(7), "nor any tick after the newest it recorded")
  }

  private companion object {
    const val LATE = 5L
    const val AHEAD = 40
    const val HOST_TICKS = 100
    const val DELAY = 6
  }
}
