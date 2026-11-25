package dev.chieppa.ease.sample

import com.github.quillraven.fleks.World.Companion.family
import dev.chieppa.ease.commands.CommandOrigin
import dev.chieppa.ease.ecs.LocalTransform
import dev.chieppa.ease.net.input.InputFrame
import dev.chieppa.ease.runtime.HostedWorld
import dev.chieppa.ease.runtime.LocalHost
import dev.chieppa.ease.runtime.loop.FixedStep
import dev.chieppa.ease.testing.AllocationBudget
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The Phase 2a gate, headless: the sample with its input script, as the player runs it, a host
 * world and a local client world over the loopback transport. The courier moves only by the
 * client's input, the client predicts it there at once, the host's log and that input give the same
 * world again, and the run allocates nothing.
 */
class SampleGateTest {
  private val fixedStep = FixedStep.perSecond()
  private val corners = listOf(2f to 0f, 2f to 2f, 0f to 2f, 0f to 0f)

  private fun scripted() = LocalHost(SampleGame(), fixedStep, input = CourierScript())

  private fun courierAt(world: HostedWorld): Pair<Float, Float> =
    with(world.world) {
      val courier = family { all(Courier) }.first()
      courier[LocalTransform].x to courier[LocalTransform].y
    }

  private fun assertAt(corner: Pair<Float, Float>, world: HostedWorld, who: String) {
    val (x, y) = courierAt(world)
    assertEquals(corner.first, x, TOLERANCE, "$who's x at tick ${world.tick}")
    assertEquals(corner.second, y, TOLERANCE, "$who's y at tick ${world.tick}")
  }

  @Test
  fun `the courier traces its loop by the client's input, predicted on the client`() {
    val local = scripted()
    val turn = CourierScript.TURN_EVERY
    var hostCorners = 0
    var clientCorners = 0

    while (hostCorners < corners.size) {
      local.step()
      val client = local.client.world
      if (client.tick == turn * (clientCorners + 1) && clientCorners < corners.size) {
        assertAt(corners[clientCorners++], client, "the client")
      }
      if (local.tick == turn * (hostCorners + 1))
        assertAt(corners[hostCorners++], local.host, "host")
    }

    assertEquals(corners.size, clientCorners, "the client reached each corner before the host")
  }

  @Test
  fun `the host logs every command with its tick and origin, and input is not a command`() {
    val local = scripted()
    repeat(RUN_TICKS) { local.step() }

    val log = local.host.history.log
    val entries = log.all
    assertEquals(SampleGame.SQUARES.size + 1, entries.size, "the squares, then the courier")
    assertTrue(entries.all { it.tick == 0L && it.origin == CommandOrigin.GAME })
    val client = local.client.world
    val clientLog = client.history.log
    assertTrue(clientLog.all.isEmpty())
  }

  @Test
  fun `replaying the log and the script's input onto a fresh world gives the same world`() {
    val local = scripted()
    repeat(RUN_TICKS) { local.step() }
    val live = local.host

    val replayed = HostedWorld(SampleGame(), fixedStep)
    val input = InputFrame(local.settings.input)
    replayed.inputs[LocalHost.LOCAL_PEER] = input
    fun stepReplayed() {
      CourierScript.steer(input, replayed.tick)
      replayed.step()
    }
    val entries = live.history.log.all
    val seeded = replayed.history.log
    entries.drop(seeded.all.size).forEach { entry ->
      while (replayed.tick < entry.tick) stepReplayed()
      replayed.execute(live.history.log.commandOf(entry), entry.origin)
    }
    while (replayed.tick < live.tick) stepReplayed()

    assertEquals(live.codec.worldToJson(live.world), replayed.codec.worldToJson(replayed.world))
  }

  @Test
  fun `a scripted run, prediction included, allocates nothing`() {
    val local = scripted()

    AllocationBudget.assertWithin(0, { local.step() }, WARMUP_TICKS, MEASURED_TICKS)
  }

  private companion object {
    const val TOLERANCE = 1e-3f
    const val RUN_TICKS = 600
    const val WARMUP_TICKS = 5_000
    const val MEASURED_TICKS = 1_000
  }
}
