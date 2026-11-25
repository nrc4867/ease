package dev.chieppa.ease.runtime

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.World.Companion.family
import dev.chieppa.ease.ecs.LocalTransform
import dev.chieppa.ease.ecs.replication.Authority
import dev.chieppa.ease.ecs.replication.PeerId
import dev.chieppa.ease.net.transport.LinkConditions
import dev.chieppa.ease.render.TRANSFORM_FLOATS
import dev.chieppa.ease.runtime.loop.FixedStep
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The local client's own walker predicted under a simulated bad network (latency, jitter, loss),
 * with the host pushing it now and then in ways the client cannot predict: it moves the tick its
 * input is given, takes the host's corrections smoothly, and ends where the host has it.
 */
class PredictionTest {
  private var gusting = true
  private val local = LocalHost(WalkModule(gusts = { gusting }), FixedStep.perSecond())
  private val client = local.client

  init {
    local.link.conditions = BAD_LINK
  }

  private fun walker(world: HostedWorld): Entity =
    with(world.world) {
      family { all(LocalTransform, Authority) }.first { it[Authority].owner != PeerId.HOST }
    }

  private fun x(world: HostedWorld): Float = with(world.world) { walker(world)[LocalTransform].x }

  private fun y(world: HostedWorld): Float = with(world.world) { walker(world)[LocalTransform].y }

  /** Steps with the walker's input at [axis], returning how many ticks the client ran. */
  private fun step(axis: Float): Long {
    client.inputFrame.setAxis(0, axis)
    val before = client.tick
    local.step()
    return client.tick - before
  }

  private fun walkPattern(tick: Long): Float = if (tick / WALK_TURN % 2 == 0L) 1f else -1f

  @Test
  fun `the walker moves the tick its input is given, before the host hears of it`() {
    gusting = false
    repeat(WARMUP) { step(0f) }
    val hostBefore = x(local.host)
    var moved = 0

    repeat(WALK) {
      val clientBefore = x(client.world)
      val ran = step(1f)
      assertEquals(clientBefore + ran * WalkModule.STEP, x(client.world), TOLERANCE)
      moved += ran.toInt()
    }

    assertTrue(moved >= WALK - 1, "the client ran a tick nearly every step, $moved of $WALK")
    assertTrue(x(local.host) < hostBefore + moved, "the host is behind the client's prediction")
  }

  @Test
  fun `corrections from the host are replayed, smoothed while drawn, and the walker converges`() {
    repeat(WARMUP) { step(0f) }
    var drawnY = Float.NaN
    val drawnTransform = FloatArray(TRANSFORM_FLOATS)
    var largestDrawnStep = 0f
    repeat(RUN) {
      step(walkPattern(client.tick))
      val id = walker(client.world).id
      val snapshot = client.world.snapshot
      snapshot.transformInto(id, 1f, drawnTransform, 0)
      val drawn = drawnTransform[1]
      if (!drawnY.isNaN()) largestDrawnStep = maxOf(largestDrawnStep, abs(drawn - drawnY))
      drawnY = drawn
    }
    gusting = false
    repeat(SETTLE) { step(0f) }

    assertTrue(client.replays > 0, "the gusts made the client replay")
    assertTrue(largestDrawnStep < WalkModule.GUST / 2, "drawn y moved $largestDrawnStep at most")
    assertEquals(x(local.host), x(client.world), TOLERANCE)
    assertEquals(y(local.host), y(client.world), TOLERANCE)
  }

  @Test
  fun `the client's predictions match the host's state tick for tick once corrected`() {
    gusting = false
    val predicted = HashMap<Long, Float>()
    val hosted = HashMap<Long, Float>()
    repeat(WARMUP) { step(0f) }
    repeat(RUN) {
      step(walkPattern(client.tick))
      predicted[client.tick - 1] = x(client.world)
      hosted[local.host.tick - 1] = x(local.host)
    }

    val compared = (2L * WARMUP until local.host.tick).filter { it in predicted }
    val wrong = compared.count { abs(predicted.getValue(it) - hosted.getValue(it)) > TOLERANCE }
    assertTrue(compared.size > RUN / 2, "compared ${compared.size} ticks")
    assertTrue(wrong * MOSTLY < compared.size, "$wrong of ${compared.size} predictions were off")
  }

  private companion object {
    val BAD_LINK = LinkConditions(latencyTicks = 4, jitterTicks = 2, loss = 0.1)
    const val WARMUP = 120
    const val WALK = 60
    const val RUN = 600
    const val SETTLE = 120
    const val WALK_TURN = 40L
    const val TOLERANCE = 1e-3f

    /** At least nine predictions in ten are right without a correction. */
    const val MOSTLY = 10
  }
}
