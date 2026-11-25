package dev.chieppa.ease.sample

import dev.chieppa.ease.gpu.GpuContext
import dev.chieppa.ease.gpu.OffscreenTarget
import dev.chieppa.ease.gpu.Pixels
import dev.chieppa.ease.gpu.lit.MeshFrame
import dev.chieppa.ease.platform.PixelSize
import dev.chieppa.ease.render.WorldRenderer
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.jupiter.api.io.TempDir

/**
 * The sample world rendered offscreen by the same renderer the player uses, through the scene's
 * camera and lit by its sun, then probed.
 */
class SampleWorldGpuTest {
  @TempDir lateinit var folder: Path

  private val size = PixelSize(320, 200)
  private val sample by lazy { SampleWorld(folder) }

  @Test
  fun `every cube shows its own color lit at its projected center over the floor`() {
    sample.step(TICKS)

    val pixels = render()

    val projection = CameraProjection(sample.content.camera(), size.width, size.height)
    val clear = Rgb.of(CLEAR.red, CLEAR.green, CLEAR.blue)
    sample.entities.forEach { entity ->
      val (column, row) = projection.pixel(sample.x(entity), sample.y(entity), sample.z(entity))
      val found = pixels.rgb(column, row)
      val expected = Rgb.brightest(sample.color(entity))
      assertTrue(Rgb.brightest(found) == expected, "cube at ($column, $row) is ${Rgb.hex(found)}")
      assertFalse(Rgb.close(found, clear), "cube at ($column, $row) is the clear color")
    }
    val (column, row) = projection.pixel(0f, 0f, FLOOR_Z)
    assertFalse(Rgb.close(pixels.rgb(column, row), clear), "no floor under the origin")
  }

  private fun render(): Pixels =
    GpuContext.open().use { gpu ->
      WorldRenderer(gpu, capacity = sample.nodes.size).use { renderer ->
        OffscreenTarget(gpu, size).use { offscreen ->
          gpu.beginFrame()
          val encoder = gpu.frameEncoder()
          renderer.render(sample.hosted.snapshot, 1f, offscreen.target, encoder)
          encoder.submit()
          offscreen.read()
        }
      }
    }

  private companion object {
    const val TICKS = 10
    const val FLOOR_Z = -0.75f
    val CLEAR = MeshFrame.DEFAULT_CLEAR
  }
}
