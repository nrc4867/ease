package dev.chieppa.ease.sample

import com.github.quillraven.fleks.World.Companion.family
import dev.chieppa.ease.commands.CommandOrigin
import dev.chieppa.ease.ecs.LocalTransform
import dev.chieppa.ease.runtime.HostedWorld
import dev.chieppa.ease.runtime.LocalHost
import dev.chieppa.ease.runtime.loop.FixedStep
import dev.chieppa.ease.testing.AllocationMeter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The Phase 2a gate, headless: the sample with its input script, as the player runs it, a host
 * world and a local client world over the loopback transport. The courier moves only by the
 * client's commands, the client sees it where the host has it, the host's log holds every change,
 * replaying the log gives the same world, and the run stays inside its allocation budgets.
 */
class SampleGateTest {
  private val fixedStep = FixedStep.perSecond()

  private fun scripted() = LocalHost(SampleGame(), fixedStep, input = CourierScript())

  private fun courierAt(world: HostedWorld): Pair<Float, Float> =
    with(world.world) {
      val courier = family { all(Courier) }.first()
      courier[LocalTransform].x to courier[LocalTransform].y
    }

  @Test
  fun `the courier traces its loop by the client's commands, and the client sees it there`() {
    val local = scripted()
    val corners = listOf(2f to 0f, 2f to 2f, 0f to 2f, 0f to 0f)

    corners.forEach { (x, y) ->
      repeat(CourierScript.STEER_EVERY.toInt()) { local.step() }
      val (atX, atY) = courierAt(local.host)
      assertEquals(x, atX, TOLERANCE, "x at tick ${local.tick}")
      assertEquals(y, atY, TOLERANCE, "y at tick ${local.tick}")
      val (seenX, seenY) = courierAt(local.client.world)
      assertEquals(atX, seenX, TOLERANCE, "the client's x at tick ${local.tick}")
      assertEquals(atY, seenY, TOLERANCE, "the client's y at tick ${local.tick}")
    }
  }

  @Test
  fun `every change is logged on the host with its tick and origin`() {
    val local = scripted()
    repeat(RUN_TICKS) { local.step() }

    val history = local.host.history
    val log = history.log.all
    val game = log.filter { it.origin == CommandOrigin.GAME }
    val steers = log.filter { it.origin == CommandOrigin.NETWORK }
    assertEquals(SampleGame.SQUARES.size + 1, game.size, "the squares, then the courier")
    assertTrue(game.all { it.tick == 0L })
    assertEquals((RUN_TICKS - 1) / CourierScript.STEER_EVERY.toInt(), steers.size)
    assertTrue(steers.all { it.tick % CourierScript.STEER_EVERY == 0L })
    val client = local.client.world
    val clientLog = client.history.log
    assertTrue(clientLog.all.isEmpty())
  }

  @Test
  fun `replaying the log onto a fresh world gives the same world`() {
    val local = scripted()
    repeat(RUN_TICKS) { local.step() }
    val live = local.host

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
    val local = scripted()
    repeat(RUN_TICKS) { local.step() }
    val window = CourierScript.STEER_EVERY.toInt()

    val report =
      AllocationMeter.measure({ repeat(window) { local.step() } }, WARMUP_WINDOWS, MEASURED_WINDOWS)

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
     * Ticks allocate nothing (`SampleTickAllocationTest`); a command sent by the client, checked by
     * the host and applied through its history is encoded, logged and decoded, which allocates.
     * This is the budget for one such command.
     */
    const val COMMAND_BUDGET_BYTES = 4_096L
  }
}
