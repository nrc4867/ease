package dev.chieppa.ease.agent

import dev.chieppa.ease.agent.tools.ToolNames
import dev.chieppa.ease.agent.tools.ToolPart
import java.io.ByteArrayInputStream
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** Capture on a real GPU: the PNG decodes to the size asked for and shows the world's quads. */
class CaptureGpuTest {
  @Test
  fun `a capture is a PNG of the requested size showing more than one color`() = runTest {
    val bench = ToolBench(StandardTestDispatcher(testScheduler))
    val args = buildJsonObject {
      put("width", WIDTH)
      put("height", HEIGHT)
    }

    val answer =
      try {
        bench.call(ToolNames.CAPTURE, args)
      } finally {
        bench.world.access { bench.capturer.close() }
      }

    val png = answer.parts.filterIsInstance<ToolPart.Png>().single()
    val image = ImageIO.read(ByteArrayInputStream(png.bytes))
    assertEquals(WIDTH, image.width)
    assertEquals(HEIGHT, image.height)
    val colors = buildSet {
      for (y in 0 until HEIGHT step STRIDE) for (x in 0 until WIDTH step STRIDE) add(
        image.getRGB(x, y)
      )
    }
    assertTrue(colors.size > 1, "the capture is one flat color")
  }

  private companion object {
    const val WIDTH = 200
    const val HEIGHT = 120
    const val STRIDE = 4
  }
}
