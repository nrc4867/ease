package dev.chieppa.ease.agent

import dev.chieppa.ease.agent.describe.DescribeTool
import dev.chieppa.ease.agent.describe.SchemaCatalog
import java.net.URLClassLoader
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** describe on a world some of whose types have no generated schema. */
class UndescribedTypesTest {
  /** A catalog that holds only a schema for the test game's own component. */
  private fun catalogOfGlow(): SchemaCatalog {
    val directory = Files.createTempDirectory("schemas")
    val file = directory.resolve(SchemaCatalog.RESOURCE)
    Files.createDirectories(file.parent)
    Files.writeString(file, "{\"test.Glow\": {\"title\": \"test.Glow\"}}")
    return SchemaCatalog.load(URLClassLoader(arrayOf(directory.toUri().toURL()), null))
  }

  @Test
  fun `describe gives the schemas it has and names the registered types it has none for`() =
    runTest {
      val bench = ToolBench(StandardTestDispatcher(testScheduler))
      val tool = DescribeTool(bench.world, catalogOfGlow())

      val answer = tool.call(JsonObject(emptyMap()))

      assertFalse(answer.error)
      val described = bench.json(answer).jsonObject
      val titles = described.list("components").map { it.text("title") }
      assertEquals(listOf("test.Glow"), titles)
      val undescribed = described.list("undescribed").map { it.jsonPrimitive.content }
      assertTrue("ease.LocalTransform" in undescribed && "ease.SetTransform" in undescribed)
      assertFalse("test.Glow" in undescribed)
    }
}
