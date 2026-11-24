package dev.chieppa.ease.sample

import dev.chieppa.ease.agent.list
import dev.chieppa.ease.agent.text
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.serialization.json.jsonObject

/**
 * The docs and ranges the sample declares reach both generated outputs: the `ease.d.ts` its build
 * assembles from every module's types, and the JSON Schemas the agent `describe` tool serves.
 */
class SampleTypesTest {
  private val agent = SampleAgent()
  private val maxSpeed = Velocity.MAX_SPEED.toInt()

  @AfterTest
  fun close() {
    agent.close()
  }

  /** The block of `ease.d.ts` that declares [serialName]. */
  private fun declaration(serialName: String): String {
    val declarations = File(checkNotNull(System.getProperty("ease.dts"))).readText()
    val marker = "// ease-type: "
    return declarations.split(marker).single { it.lineSequence().first() == serialName }
  }

  private fun schema(group: String, serialName: String) =
    agent
      .json(agent.call("describe"))
      .list(group)
      .single { it.text("title") == serialName }
      .jsonObject

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
    val steer = declaration("sample.Steer")

    assertTrue("The entity to steer." in steer, steer)
    assertTrue("@minimum -$maxSpeed" in steer, steer)
    assertTrue("@maximum $maxSpeed" in steer, steer)
  }

  @Test
  fun `ease dot d dot ts holds the engine's types beside the sample's`() {
    assertTrue("@minimum -1" in declaration("ease.LocalTransform"))
    assertTrue("Red channel" in declaration("ease.Quad"))
    assertTrue("type: \"ease.Destroy\"" in declaration("ease.Destroy"))
  }

  @Test
  fun `a doc and a range of a sample component reach its JSON Schema`() {
    val velocity = schema("components", "sample.Velocity")

    val x = velocity.getValue("properties").jsonObject.getValue("x").jsonObject
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
    val steer = schema("commands", "sample.Steer")

    val y = steer.getValue("properties").jsonObject.getValue("y").jsonObject
    assertEquals("Speed along y, in world units a second.", y.text("description"))
    assertEquals(-maxSpeed, y.text("minimum").toInt())
    assertEquals(maxSpeed, y.text("maximum").toInt())
  }
}
