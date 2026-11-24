package dev.chieppa.ease.sample

import dev.chieppa.ease.agent.list
import dev.chieppa.ease.agent.names
import dev.chieppa.ease.agent.text
import java.io.ByteArrayInputStream
import java.util.Base64
import javax.imageio.ImageIO
import kotlin.math.abs
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * What an agent needs first, over MCP against the sample game: list the component types, read the
 * world, and get a captured frame of it.
 */
class SampleAgentGpuTest {
  private val agent = SampleAgent()

  @AfterTest
  fun close() {
    agent.close()
  }

  @Test
  fun `an agent lists component types, reads the world and captures a frame`() {
    val tools = agent.client.listTools().names()
    assertEquals(listOf("describe", "query", "capture", "logs", "test"), tools)

    val names = agent.json(agent.call("describe")).list("components").names()
    assertTrue(names.containsAll(listOf("ease.LocalTransform", "ease.Quad", "sample.Velocity")))

    val world = agent.json(agent.call("query")).list("entities")
    assertEquals(SampleGame.SQUARES.size, world.size)

    val capture =
      agent.call(
        "capture",
        buildJsonObject {
          put("width", WIDTH)
          put("height", HEIGHT)
        },
      )
    val image = agent.content(capture).single { it.text("type") == "image" }
    assertEquals("image/png", image.text("mimeType"))
    val png = Base64.getDecoder().decode(image.text("data"))
    val frame = ImageIO.read(ByteArrayInputStream(png))
    assertEquals(WIDTH, frame.width)
    assertEquals(HEIGHT, frame.height)
    val pixelsPerUnit = HEIGHT / Playfield.VISIBLE_HEIGHT
    SampleGame.SQUARES.forEach { square ->
      val column = (WIDTH / 2f + square.x * pixelsPerUnit).toInt()
      val row = (HEIGHT / 2f - square.y * pixelsPerUnit).toInt()
      val found = frame.getRGB(column, row)
      assertTrue(close(found, square.rgb), "square at (${square.x}, ${square.y})")
    }
  }

  /** Whether every channel of [a] and [b] is within one step, as rounding allows. */
  private fun close(a: Int, b: Int): Boolean =
    listOf(RED_SHIFT, GREEN_SHIFT, 0).all { shift ->
      abs((a shr shift and CHANNEL) - (b shr shift and CHANNEL)) <= 1
    }

  private companion object {
    const val WIDTH = 320
    const val HEIGHT = 180
    const val RED_SHIFT = 16
    const val GREEN_SHIFT = 8
    const val CHANNEL = 0xFF
  }
}
