package dev.chieppa.ease.sample

import dev.chieppa.ease.gpu.Color
import dev.chieppa.ease.gpu.CommandEncoder
import dev.chieppa.ease.gpu.GpuContext
import dev.chieppa.ease.gpu.InstancedQuadPass
import dev.chieppa.ease.gpu.OffscreenTarget
import dev.chieppa.ease.gpu.Pixels
import dev.chieppa.ease.platform.PixelSize
import dev.chieppa.ease.render.WorldRenderer
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertTrue
import org.junit.jupiter.api.io.TempDir

/** The sample world rendered offscreen by the same renderer the player uses, then probed. */
class SampleWorldGpuTest {
  @TempDir lateinit var folder: Path

  private val size = PixelSize(320, 180)
  private val sample by lazy { SampleWorld(folder) }
  private val camera by lazy { sample.module.camera() }
  private val pixelsPerUnit by lazy { size.height / camera.visibleHeight }

  @Test
  fun `every square shows its color at its projected center and the corners stay clear`() {
    sample.step(TICKS)

    val pixels = render()

    sample.entities.forEach { entity ->
      val column = pixelX(sample.x(entity))
      val row = pixelY(sample.y(entity))
      val found = pixels.rgb(column, row)
      assertTrue(
        Rgb.close(found, sample.rgb(entity)),
        "square at ($column, $row) is ${Rgb.hex(found)}",
      )
    }
    val clear = rgbOf(InstancedQuadPass.DEFAULT_CLEAR)
    val corners =
      listOf(0 to 0, size.width - 1 to 0, 0 to size.height - 1, size.width - 1 to size.height - 1)
    for ((column, row) in corners) {
      assertTrue(Rgb.close(pixels.rgb(column, row), clear), "corner ($column, $row) is not clear")
    }
  }

  private fun render(): Pixels =
    GpuContext.open().use { gpu ->
      WorldRenderer(gpu, capacity = sample.entities.size).use { renderer ->
        OffscreenTarget(gpu, size).use { offscreen ->
          val encoder = CommandEncoder.on(gpu)
          renderer.render(sample.hosted.snapshot, 1f, camera, offscreen.target, encoder)
          encoder.submit()
          offscreen.read()
        }
      }
    }

  private fun pixelX(x: Float): Int =
    (size.width / 2f + (x - camera.centerX) * pixelsPerUnit).toInt()

  private fun pixelY(y: Float): Int =
    (size.height / 2f - (y - camera.centerY) * pixelsPerUnit).toInt()

  private fun rgbOf(color: Color): Int = Rgb.of(color.red, color.green, color.blue)

  private companion object {
    const val TICKS = 10
  }
}
