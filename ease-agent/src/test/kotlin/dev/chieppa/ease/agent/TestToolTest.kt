package dev.chieppa.ease.agent

import dev.chieppa.ease.agent.tools.ToolNames
import dev.chieppa.ease.commands.CommandOrigin
import dev.chieppa.ease.commands.builtin.SetTransform
import dev.chieppa.ease.ecs.LocalTransform
import dev.chieppa.ease.ecs.replication.networkIdOf
import dev.chieppa.ease.math.Quat
import dev.chieppa.ease.math.Transform
import dev.chieppa.ease.math.Vec3
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/** The test tool's checks on a live world that was played and changed through commands. */
class TestToolTest {
  private suspend fun played(bench: ToolBench) {
    bench.stepLive(TICKS_BEFORE)
    bench.world.access { hosted ->
      val still = with(hosted.world) { asEntityBag().first { !it.has(Glow) } }
      val moved = Transform(Vec3(0f, 1f, 0f), Quat.IDENTITY, Vec3(1f, 1f, 1f))
      hosted.execute(SetTransform(hosted.world.networkIdOf(still), moved), CommandOrigin.AGENT)
    }
    bench.stepLive(TICKS_AFTER)
  }

  private suspend fun run(bench: ToolBench, check: String, ticks: Int? = null): JsonObject {
    val args = buildJsonObject {
      put("check", check)
      ticks?.let { put("ticks", it) }
    }
    return bench.json(bench.call(ToolNames.TEST, args)).jsonObject
  }

  @Test
  fun `replay passes on a world changed only through commands`() = runTest {
    val bench = ToolBench(StandardTestDispatcher(testScheduler))
    played(bench)

    val result = run(bench, "replay")

    assertTrue(result.getValue("passed").jsonPrimitive.boolean, result.toString())
    assertEquals(TICKS_BEFORE + TICKS_AFTER, result.getValue("tick").jsonPrimitive.int)
  }

  @Test
  fun `replay fails, naming the difference, when the world changed outside the log`() = runTest {
    val bench = ToolBench(StandardTestDispatcher(testScheduler))
    played(bench)
    bench.world.access { hosted ->
      with(hosted.world) { asEntityBag().first { it.has(Glow) }[LocalTransform].y = 5f }
    }

    val result = run(bench, "replay")

    assertEquals("false", result.text("passed"))
    val failures = result.list("failures")
    val failure = failures.single().jsonPrimitive.content
    assertTrue(".y: live 5.0, replayed 0.0" in failure, failure)
  }

  @Test
  fun `ticks runs a scratch copy and leaves the live world where it was`() = runTest {
    val bench = ToolBench(StandardTestDispatcher(testScheduler))
    played(bench)

    val result = run(bench, "ticks", SCRATCH_TICKS)

    assertTrue(result.getValue("passed").jsonPrimitive.boolean, result.toString())
    val reached = TICKS_BEFORE + TICKS_AFTER + SCRATCH_TICKS
    assertEquals(reached, result.getValue("tick").jsonPrimitive.int)
    assertEquals((TICKS_BEFORE + TICKS_AFTER).toLong(), bench.world.access { it.tick })
    val lines = bench.log.since(0).lines
    val logged = lines.map { it.text }
    assertTrue(logged.any { "ran $SCRATCH_TICKS of $SCRATCH_TICKS" in it }, logged.toString())
  }

  private companion object {
    const val TICKS_BEFORE = 4
    const val TICKS_AFTER = 3
    const val SCRATCH_TICKS = 20
  }
}
