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
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

/** describe and query on the drift game's world. */
class ReadToolsTest {
  private fun types(described: JsonElement, group: String): Map<String, JsonObject> =
    described.list(group).map { it.jsonObject }.associateBy { it.text("title") }

  private fun properties(type: JsonObject): JsonObject = type.getValue("properties").jsonObject

  private fun property(type: JsonObject, name: String): JsonObject =
    properties(type).getValue(name).jsonObject

  @Test
  fun `describe lists the engine's and the game's types as schemas with their fields`() = runTest {
    val bench = ToolBench(StandardTestDispatcher(testScheduler))

    val described = bench.json(bench.call(ToolNames.DESCRIBE))

    val components = types(described, "components")
    assertEquals("number", property(components.getValue("ease.LocalTransform"), "x").text("type"))
    assertEquals("number", property(components.getValue("ease.Quad"), "size").text("type"))
    assertEquals("number", property(components.getValue("test.Glow"), "level").text("type"))
    val setTransform = types(described, "commands").getValue("ease.SetTransform")
    assertEquals(setOf("type", "entity", "transform"), properties(setTransform).keys)
    val defs = setTransform.getValue("\$defs").jsonObject
    val transform = defs.getValue("ease.Transform").jsonObject
    assertEquals(setOf("translation", "rotation", "scale"), properties(transform).keys)
  }

  @Test
  fun `describe carries the docs and ranges the code declares`() = runTest {
    val bench = ToolBench(StandardTestDispatcher(testScheduler))

    val described = bench.json(bench.call(ToolNames.DESCRIBE))

    val localTransform = types(described, "components").getValue("ease.LocalTransform")
    val rotation = property(localTransform, "rotationX")
    assertEquals("-1", rotation.text("minimum"))
    assertEquals("1", rotation.text("maximum"))
    assertTrue(rotation.text("description").isNotBlank())
    assertTrue(localTransform.text("description").isNotBlank())
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
