package dev.chieppa.ease.runtime

import dev.chieppa.ease.commands.CommandOrigin
import dev.chieppa.ease.commands.builtin.Spawn
import dev.chieppa.ease.ecs.LocalTransform
import dev.chieppa.ease.net.prediction.ClockSettings
import dev.chieppa.ease.platform.time.ManualClock
import dev.chieppa.ease.render.TRANSFORM_FLOATS
import dev.chieppa.ease.runtime.loop.FixedStep
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class HostedWorldTest {
  private val fixedStep = FixedStep.perSecond()
  private val hosted = HostedWorld(DriftModule(), fixedStep)

  private fun logged() = hosted.history.log.all

  private fun driftedX(): Float = with(hosted.world) { asEntityBag().first()[LocalTransform].x }

  @Test
  fun `the module populates the world through logged commands`() {
    val entry = logged().single()

    assertEquals(0, entry.tick)
    assertEquals(CommandOrigin.GAME, entry.origin)
    assertEquals(1, hosted.world.numEntities)
  }

  @Test
  fun `each step runs the systems, then the extract, and counts a tick`() {
    repeat(STEPS) { hosted.step() }

    assertEquals(STEPS.toLong(), hosted.tick)
    assertEquals(STEPS.toFloat(), driftedX())
    val id = hosted.snapshot.entityAt(0)
    val transform = FloatArray(TRANSFORM_FLOATS)
    hosted.snapshot.transformInto(id, 1f, transform, 0)
    assertEquals(STEPS.toFloat(), transform[0])
    hosted.snapshot.transformInto(id, 0f, transform, 0)
    assertEquals(STEPS - 1f, transform[0])
  }

  @Test
  fun `commands are stamped with the tick they run on`() {
    repeat(STEPS) { hosted.step() }

    hosted.execute(Spawn(listOf(LocalTransform())), CommandOrigin.AGENT)

    val entry = logged().last()
    assertEquals(STEPS.toLong(), entry.tick)
    assertEquals(CommandOrigin.AGENT, entry.origin)
  }

  @Test
  fun `the runner ticks host and client as the clock moves and leaves the rest as alpha`() {
    val clock = ManualClock()
    val local = LocalHost(DriftModule(), fixedStep)
    val runner = WorldRunner(local, clock)
    runner.frame()

    clock.advance(fixedStep.stepNanos * 2 + fixedStep.stepNanos / 2)
    val steps = runner.frame()

    assertEquals(2, steps)
    assertEquals(2, local.host.tick)
    assertEquals(2 + ClockSettings.DEFAULT_BUFFER_TICKS.toLong(), local.client.tick)
    assertEquals(0.5f, runner.alpha, ALPHA_TOLERANCE)
  }

  @Test
  fun `loading a game fails clearly when none is on the classpath`() {
    val failure = assertFailsWith<IllegalStateException> { GameModules.load() }

    assertEquals(true, failure.message?.startsWith("No game on the classpath"))
  }

  private companion object {
    const val STEPS = 3
    const val ALPHA_TOLERANCE = 1e-4f
  }
}
