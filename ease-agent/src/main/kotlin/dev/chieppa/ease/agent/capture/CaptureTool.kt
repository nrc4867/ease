package dev.chieppa.ease.agent.capture

import dev.chieppa.ease.agent.AgentWorld
import dev.chieppa.ease.agent.tools.AgentTool
import dev.chieppa.ease.agent.tools.ToolAnswer
import dev.chieppa.ease.agent.tools.ToolArgs
import dev.chieppa.ease.agent.tools.ToolInput
import dev.chieppa.ease.agent.tools.ToolNames
import dev.chieppa.ease.agent.tools.ToolPart
import dev.chieppa.ease.platform.PixelSize
import kotlinx.serialization.json.JsonObject

/** A PNG of the live world through its camera. */
public class CaptureTool(private val world: AgentWorld, private val capturer: Capturer) :
  AgentTool {
  override val name: String = ToolNames.CAPTURE
  override val description: String =
    "Renders the live world through its camera, the Camera of the lowest entity id, lit by " +
      "its DirectionalLight, into a PNG of the requested size (${DEFAULT_WIDTH}x$DEFAULT_HEIGHT " +
      "when absent); without a camera the image is the clear color. Needs a GPU on the host."
  override val input: ToolInput = ToolInput.of {
    integer(WIDTH, "Image width in pixels.", 1L..MAX_WIDTH)
    integer(HEIGHT, "Image height in pixels.", 1L..MAX_HEIGHT)
  }
  override val readOnly: Boolean = true

  override suspend fun call(args: JsonObject): ToolAnswer {
    val size =
      PixelSize(
        (ToolArgs.int(args, WIDTH) ?: DEFAULT_WIDTH).coerceIn(1, MAX_WIDTH.toInt()),
        (ToolArgs.int(args, HEIGHT) ?: DEFAULT_HEIGHT).coerceIn(1, MAX_HEIGHT.toInt()),
      )
    val (png, tick) =
      world.access { hosted -> PngImages.encode(capturer.capture(hosted, size)) to hosted.tick }
    val caption = "Tick $tick, ${size.width}x${size.height}"
    return ToolAnswer(listOf(ToolPart.Png(png), ToolPart.Text(caption)))
  }

  private companion object {
    const val WIDTH = "width"
    const val HEIGHT = "height"
    const val DEFAULT_WIDTH = 640
    const val DEFAULT_HEIGHT = 360
    const val MAX_WIDTH = 1920L
    const val MAX_HEIGHT = 1080L
  }
}
