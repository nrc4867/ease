package dev.chieppa.ease.agent

import dev.chieppa.ease.agent.tools.ToolAnswer
import dev.chieppa.ease.agent.tools.ToolNames
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

/** describe and query on the drift game's world. */
class ReadToolsTest {
  private fun types(described: JsonElement, group: String): Map<String, JsonElement> =
    described.list(group).associateBy { it.text("name") }

  private fun fields(type: JsonElement): Map<String, JsonElement> =
    type.list("fields").associate { it.text("name") to it.field("type") }

  private fun kind(type: JsonElement?): String = checkNotNull(type).text("kind")

  @Test
  fun `describe lists the engine's and the game's types with their fields`() = runTest {
    val bench = ToolBench(StandardTestDispatcher(testScheduler))

    val described = bench.json(bench.call(ToolNames.DESCRIBE))

    val components = types(described, "components")
    assertEquals("number", kind(fields(components.getValue("ease.LocalTransform"))["x"]))
    assertEquals("number", kind(fields(components.getValue("ease.Quad"))["size"]))
    assertEquals("number", kind(fields(components.getValue("test.Glow"))["level"]))
    val setTransform = fields(types(described, "commands").getValue("ease.SetTransform"))
    assertEquals(setOf("entity", "transform"), setTransform.keys)
    val translation = fields(checkNotNull(setTransform["transform"]))["translation"]
    assertEquals(setOf("x", "y", "z"), fields(checkNotNull(translation)).keys)
  }

  @Test
  fun `query gives every entity with its components and the tick`() = runTest {
    val bench = ToolBench(StandardTestDispatcher(testScheduler))
    bench.stepLive(TICKS)

    val queried = bench.json(bench.call(ToolNames.QUERY)).jsonObject

    assertEquals(TICKS, queried.getValue("tick").jsonPrimitive.int)
    val entities = queried.getValue("entities").jsonArray
    assertEquals(2, entities.size)
    val types = entities.map { componentTypes(it) }
    val drawn = setOf("ease.LocalTransform", "ease.Quad", "ease.Authority")
    assertEquals(drawn + "test.Glow", types[0])
    assertEquals(drawn, types[1])
  }

  @Test
  fun `query narrows to a component type or to entity ids`() = runTest {
    val bench = ToolBench(StandardTestDispatcher(testScheduler))

    val glowing = bench.call(ToolNames.QUERY, buildJsonObject { put("component", "test.Glow") })
    val first = bench.json(bench.call(ToolNames.QUERY)).list("entities")[0]
    val firstNetworkId = first.field("entity")
    val firstId = buildJsonObject { putJsonArray("entities") { add(firstNetworkId) } }
    val byId = bench.call(ToolNames.QUERY, firstId)

    assertEquals(1, entitiesOf(bench, glowing).size)
    assertEquals(JsonArray(listOf(first)), entitiesOf(bench, byId))
  }

  private fun entitiesOf(bench: ToolBench, answer: ToolAnswer) = bench.json(answer).list("entities")

  private fun componentTypes(entity: JsonElement): Set<String> {
    val text = entity.toString()
    val found = Regex("\"type\":\"([\\w.]+)\"").findAll(text).map { it.groupValues[1] }.toSet()
    assertTrue(found.isNotEmpty(), text)
    return found
  }

  private companion object {
    const val TICKS = 3
  }
}
