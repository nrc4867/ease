package dev.chieppa.ease.agent.capture

import dev.chieppa.ease.agent.AgentWorld
import dev.chieppa.ease.agent.tools.AgentTool
import dev.chieppa.ease.agent.tools.ToolAnswer
import dev.chieppa.ease.agent.tools.ToolArgs
import dev.chieppa.ease.agent.tools.ToolInput
import dev.chieppa.ease.agent.tools.ToolNames
import dev.chieppa.ease.agent.tools.ToolPart
import dev.chieppa.ease.agent.tools.ToolRefusal
import dev.chieppa.ease.platform.PixelSize
import kotlinx.serialization.json.JsonObject

/** A PNG of the live world through the game's camera, or one moved by the arguments. */
public class CaptureTool(private val world: AgentWorld, private val capturer: Capturer) :
  AgentTool {
  override val name: String = ToolNames.CAPTURE
  override val description: String =
    "Renders the live world through the game's camera into a PNG of the requested size " +
      "(${DEFAULT_WIDTH}x$DEFAULT_HEIGHT when absent). centerX, centerY and visibleHeight, in " +
      "world units, move the camera for this capture only. Needs a GPU on the host."
  override val input: ToolInput = ToolInput.of {
    integer(WIDTH, "Image width in pixels.", 1L..MAX_WIDTH)
    integer(HEIGHT, "Image height in pixels.", 1L..MAX_HEIGHT)
    number(CENTER_X, "Camera center x in world units; the game's camera when absent.")
    number(CENTER_Y, "Camera center y in world units; the game's camera when absent.")
    number(VISIBLE_HEIGHT, "World units shown top to bottom; the game's camera when absent.")
  }
  override val readOnly: Boolean = true

  override suspend fun call(args: JsonObject): ToolAnswer {
    val size =
      PixelSize(
        (ToolArgs.int(args, WIDTH) ?: DEFAULT_WIDTH).coerceIn(1, MAX_WIDTH.toInt()),
        (ToolArgs.int(args, HEIGHT) ?: DEFAULT_HEIGHT).coerceIn(1, MAX_HEIGHT.toInt()),
      )
    val camera = world.module.camera()
    ToolArgs.float(args, CENTER_X)?.let { camera.centerX = it }
    ToolArgs.float(args, CENTER_Y)?.let { camera.centerY = it }
    ToolArgs.float(args, VISIBLE_HEIGHT)?.let {
      if (it <= 0f) throw ToolRefusal("$VISIBLE_HEIGHT must be above 0, got $it")
      camera.visibleHeight = it
    }
    val (png, tick) =
      world.access { hosted ->
        PngImages.encode(capturer.capture(hosted, camera, size)) to hosted.tick
      }
    val caption =
      "Tick $tick, ${size.width}x${size.height}, camera at (${camera.centerX}, " +
        "${camera.centerY}) showing ${camera.visibleHeight} units"
    return ToolAnswer(listOf(ToolPart.Png(png), ToolPart.Text(caption)))
  }

  private companion object {
    const val WIDTH = "width"
    const val HEIGHT = "height"
    const val CENTER_X = "centerX"
    const val CENTER_Y = "centerY"
    const val VISIBLE_HEIGHT = "visibleHeight"
    const val DEFAULT_WIDTH = 640
    const val DEFAULT_HEIGHT = 360
    const val MAX_WIDTH = 1920L
    const val MAX_HEIGHT = 1080L
  }
}
