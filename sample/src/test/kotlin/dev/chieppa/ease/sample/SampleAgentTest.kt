package dev.chieppa.ease.sample

import dev.chieppa.ease.agent.list
import dev.chieppa.ease.agent.text
import java.nio.file.Path
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import org.junit.jupiter.api.io.TempDir

/** describe and query on the sample world over MCP; no GPU needed. */
class SampleAgentTest {
  @TempDir lateinit var folder: Path

  private val started = lazy { SampleAgent(folder) }
  private val agent by started

  @AfterTest
  fun close() {
    if (started.isInitialized()) agent.close()
  }

  private fun fieldNames(types: JsonElement, name: String): Set<String> {
    val type = types.jsonArray.single { it.text("title") == name }.jsonObject
    return type.getValue("properties").jsonObject.keys - "type"
  }

  @Test
  fun `describe lists the engine's types and the sample's own, with their fields`() {
    val described = agent.json(agent.call("describe")).jsonObject

    val components = described.getValue("components")
    assertTrue(fieldNames(components, "ease.LocalTransform").containsAll(listOf("x", "y", "z")))
    assertEquals(setOf("shape"), fieldNames(components, "ease.Mesh"))
    assertEquals(setOf("x", "y"), fieldNames(components, "sample.Velocity"))
    val commands = described.getValue("commands")
    assertEquals(setOf("entity", "transform"), fieldNames(commands, "ease.SetTransform"))
  }

  @Test
  fun `query gives every node, the cubes with their transform, mesh and velocity`() {
    val queried = agent.json(agent.call("query")).jsonObject

    assertEquals(0, queried.text("tick").toInt())
    val entities = queried.list("entities")
    assertEquals(agent.content.placed().size, entities.size)
    val cubes = entities.map { it.toString() }.filter { "\"type\":\"sample.Velocity\"" in it }
    assertEquals(agent.content.cubes().size, cubes.size)
    cubes.forEach { text ->
      listOf("ease.LocalTransform", "ease.Mesh", "ease.Material").forEach { type ->
        assertTrue("\"type\":\"$type\"" in text, text)
      }
    }
  }
}
