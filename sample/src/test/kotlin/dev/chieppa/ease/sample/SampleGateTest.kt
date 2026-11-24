package dev.chieppa.ease.sample

import com.github.quillraven.fleks.World.Companion.family
import dev.chieppa.ease.commands.CommandOrigin
import dev.chieppa.ease.ecs.LocalTransform
import dev.chieppa.ease.runtime.HostedWorld
import dev.chieppa.ease.runtime.loop.FixedStep
import dev.chieppa.ease.testing.AllocationMeter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The Phase 2a gate, headless: the sample with its input script, as the player runs it. The courier
 * moves only by commands, the log holds every change, replaying the log gives the same world, and
 * the run stays inside its allocation budgets.
 */
class SampleGateTest {
  private val fixedStep = FixedStep.perSecond()

  private fun scripted() = HostedWorld(SampleGame(), fixedStep, input = CourierScript())

  private fun courierAt(world: HostedWorld): Pair<Float, Float> =
    with(world.world) {
      val courier = family { all(Courier) }.first()
      courier[LocalTransform].x to courier[LocalTransform].y
    }

  @Test
  fun `the courier traces its loop by commands`() {
    val world = scripted()
    val corners = listOf(2f to 0f, 2f to 2f, 0f to 2f, 0f to 0f)

    corners.forEach { (x, y) ->
      repeat(CourierScript.STEER_EVERY.toInt()) { world.step() }
      val (atX, atY) = courierAt(world)
      assertEquals(x, atX, TOLERANCE, "x at tick ${world.tick}")
      assertEquals(y, atY, TOLERANCE, "y at tick ${world.tick}")
    }
  }

  @Test
  fun `every scripted change is logged with its tick and origin`() {
    val world = scripted()
    repeat(RUN_TICKS) { world.step() }

    val log = world.history.log
    val scripted = log.all.filter { it.origin == CommandOrigin.SCRIPT }
    val steers = (RUN_TICKS - 1) / CourierScript.STEER_EVERY.toInt()
    assertEquals(1 + steers, scripted.size)
    assertEquals(0L, scripted.first().tick)
    assertTrue(scripted.drop(1).all { it.tick % CourierScript.STEER_EVERY == 0L })
  }

  @Test
  fun `replaying the log onto a fresh world gives the same world`() {
    val live = scripted()
    repeat(RUN_TICKS) { live.step() }

    val replayed = HostedWorld(SampleGame(), fixedStep)
    val entries = live.history.log.all
    val seeded = replayed.history.log
    entries.drop(seeded.all.size).forEach { entry ->
      while (replayed.tick < entry.tick) replayed.step()
      replayed.execute(live.history.log.commandOf(entry), entry.origin)
    }
    while (replayed.tick < live.tick) replayed.step()

    assertEquals(live.codec.worldToJson(live.world), replayed.codec.worldToJson(replayed.world))
  }

  @Test
  fun `a scripted run stays inside its allocation budgets`() {
    val world = scripted()
    repeat(RUN_TICKS) { world.step() }
    val window = CourierScript.STEER_EVERY.toInt()

    val report =
      AllocationMeter.measure({ repeat(window) { world.step() } }, WARMUP_WINDOWS, MEASURED_WINDOWS)

    assertTrue(
      report.bytesPerCall() <= COMMAND_BUDGET_BYTES,
      "${window} ticks with one command allocated ${report.bytesPerCall()} bytes; " +
        "the budget is $COMMAND_BUDGET_BYTES",
    )
  }

  private companion object {
    const val TOLERANCE = 1e-3f
    const val RUN_TICKS = 600
    const val WARMUP_WINDOWS = 200
    const val MEASURED_WINDOWS = 50

    /**
     * Ticks allocate nothing (`SampleTickAllocationTest`); a command applied through the history is
     * encoded, logged and decoded, which allocates. This is the budget for one such command.
     */
    const val COMMAND_BUDGET_BYTES = 4_096L
  }
}
