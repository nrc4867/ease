package dev.chieppa.ease.agent

import dev.chieppa.ease.agent.tools.ToolNames
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlinx.serialization.json.put

/** Reading the host's log page by page through its marker. */
class LogsToolTest {
  private suspend fun page(bench: ToolBench, marker: Long, limit: Int): JsonObject {
    val args = buildJsonObject {
      put("marker", marker)
      put("limit", limit)
    }
    return bench.json(bench.call(ToolNames.LOGS, args)).jsonObject
  }

  private fun texts(page: JsonObject): List<String> = page.list("lines").map { it.text("text") }

  @Test
  fun `the next marker reads only the lines after the last page`() = runTest {
    val bench = ToolBench(StandardTestDispatcher(testScheduler))
    listOf("alpha", "beta", "gamma").forEach { bench.log.record(it) }

    val first = page(bench, marker = 0, limit = 2)
    val second = page(bench, first.getValue("next").jsonPrimitive.long, limit = 2)
    bench.log.record("delta")
    val third = page(bench, second.getValue("next").jsonPrimitive.long, limit = 2)

    assertEquals(listOf("alpha", "beta"), texts(first))
    assertEquals(listOf("gamma"), texts(second))
    assertEquals(listOf("delta"), texts(third))
    assertEquals(4L, third.getValue("next").jsonPrimitive.long)
  }

  @Test
  fun `the log keeps its newest lines and counts those dropped`() {
    val log = HostLog(capacity = 2)
    listOf("one", "two", "three").forEach { log.record(it) }

    val page = log.since(0)

    assertEquals(listOf("two", "three"), page.lines.map { it.text })
    assertEquals(1L, page.dropped)
    assertEquals(3L, page.next)
  }

  @Test
  fun `loading the game is logged`() = runTest {
    val bench = ToolBench(StandardTestDispatcher(testScheduler))

    bench.world.access {}

    val lines = bench.log.since(0).lines
    val loaded = lines.single().text
    assertEquals("Game loaded: ${DriftGame::class.java.name}, 4 entities at tick 0", loaded)
  }
}
