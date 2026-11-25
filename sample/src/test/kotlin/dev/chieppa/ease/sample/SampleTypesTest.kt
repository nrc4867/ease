package dev.chieppa.ease.sample

import dev.chieppa.ease.agent.list
import dev.chieppa.ease.agent.text
import dev.chieppa.ease.annotations.GeneratedFiles
import java.io.File
import java.nio.file.Path
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import org.junit.jupiter.api.io.TempDir

/**
 * The docs and ranges the sample declares reach both generated outputs: the `ease.d.ts` its build
 * assembles from every module's types, and the JSON Schemas the agent `describe` tool serves.
 */
class SampleTypesTest {
  @TempDir lateinit var folder: Path

  private val started = lazy { SampleAgent(folder) }
  private val agent by started
  private val maxSpeed = Velocity.MAX_SPEED.toInt()
  private val maxPace = Courier.MAX_PACE.toInt()

  @AfterTest
  fun close() {
    if (started.isInitialized()) agent.close()
  }

  /** The block of `ease.d.ts` that declares [serialName]. */
  private fun declaration(serialName: String): String {
    val declarations = File(checkNotNull(System.getProperty("ease.dts"))).readText()
    val block = GeneratedFiles.DECLARED_MARKER + serialName + "\n"
    val start = declarations.indexOf(block)
    check(start >= 0) { "$serialName is not declared in ease.d.ts" }
    val next =
      Regex(
        Regex.escape(GeneratedFiles.DECLARED_MARKER) +
          "|" +
          Regex.escape(GeneratedFiles.REACHED_MARKER)
      )
    return declarations.substring(start + block.length).split(next).first()
  }

  private fun schema(group: String, serialName: String) =
    agent
      .json(agent.call("describe"))
      .list(group)
      .single { it.text("title") == serialName }
      .jsonObject

  private fun property(schema: JsonObject, name: String): JsonObject {
    val properties = schema.getValue("properties").jsonObject
    return properties.getValue(name).jsonObject
  }

  @Test
  fun `a doc and a range of a sample component reach ease dot d dot ts`() {
    val velocity = declaration("sample.Velocity")

    assertTrue("How fast an entity moves" in velocity, velocity)
    assertTrue("Speed along x, in world units a second." in velocity, velocity)
    assertTrue("@minimum -$maxSpeed" in velocity, velocity)
    assertTrue("@maximum $maxSpeed" in velocity, velocity)
  }

  @Test
  fun `a doc and a range of a sample command reach ease dot d dot ts`() {
    val setPace = declaration("sample.SetPace")

    assertTrue("The new pace, in world units a second." in setPace, setPace)
    assertTrue("@minimum 0" in setPace, setPace)
    assertTrue("@maximum $maxPace" in setPace, setPace)
  }

  @Test
  fun `ease dot d dot ts holds the engine's types beside the sample's`() {
    assertTrue("@minimum -1" in declaration("ease.LocalTransform"))
    assertTrue("Base color red" in declaration("ease.Material"))
    assertTrue("type: \"ease.Destroy\"" in declaration("ease.Destroy"))
  }

  @Test
  fun `a doc and a range of a sample component reach its JSON Schema`() {
    val velocity = schema("components", "sample.Velocity")

    val x = property(velocity, "x")
    assertEquals(
      "How fast an entity moves, in world units a second along x and y.",
      velocity.text("description"),
    )
    assertEquals("Speed along x, in world units a second.", x.text("description"))
    assertEquals(-maxSpeed, x.text("minimum").toInt())
    assertEquals(maxSpeed, x.text("maximum").toInt())
  }

  @Test
  fun `a doc and a range of a sample command reach its JSON Schema`() {
    val setPace = schema("commands", "sample.SetPace")

    val pace = property(setPace, "pace")
    assertEquals("The new pace, in world units a second.", pace.text("description"))
    assertEquals(0, pace.text("minimum").toInt())
    assertEquals(maxPace, pace.text("maximum").toInt())
  }
}
