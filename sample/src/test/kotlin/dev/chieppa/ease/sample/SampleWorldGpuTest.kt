package dev.chieppa.ease.sample

import dev.chieppa.ease.gpu.GpuContext
import dev.chieppa.ease.gpu.OffscreenTarget
import dev.chieppa.ease.gpu.Pixels
import dev.chieppa.ease.gpu.lit.MeshFrame
import dev.chieppa.ease.platform.PixelSize
import dev.chieppa.ease.render.RenderSnapshot
import dev.chieppa.ease.render.WorldRenderer
import dev.chieppa.ease.runtime.LocalHost
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

  @Test
  fun `the imported car shows past the playfield with its near wheel`() {
    sample.step(TICKS)

    val pixels = render()

    assertTrue(sample.hosted.assetProblems.isEmpty(), "${sample.hosted.assetProblems}")
    assertCarIn(pixels)
  }

  @Test
  fun `the player's client world draws the imported car too`() {
    val local = LocalHost(SampleGame(), sample.fixedStep, assets = sample.content.assets)
    repeat(CLIENT_TICKS) { local.step() }

    val pixels = render(local.snapshot)

    val problems = local.client.world.assetProblems
    assertTrue(problems.isEmpty(), "$problems")
    assertCarIn(pixels)
  }

  private fun assertCarIn(pixels: Pixels) {
    val projection = CameraProjection(sample.content.camera(), size.width, size.height)
    val clear = Rgb.of(CLEAR.red, CLEAR.green, CLEAR.blue)
    val (bodyColumn, bodyRow) = projection.pixel(0f, CAR_Y, CAR_Z + CAR_BODY_HEIGHT)
    val (aboveColumn, aboveRow) = projection.pixel(0f, CAR_Y, CAR_Z + CAR_ABOVE)
    val (wheelColumn, wheelRow) =
      projection.pixel(CAR_WHEEL_X, CAR_Y - CAR_WHEEL_SIDE, CAR_Z + CAR_WHEEL_HEIGHT)
    val body = pixels.rgb(bodyColumn, bodyRow)
    val wheel = pixels.rgb(wheelColumn, wheelRow)
    assertFalse(Rgb.close(body, clear), "no car body at ($bodyColumn, $bodyRow)")
    assertFalse(Rgb.close(wheel, clear), "no wheel at ($wheelColumn, $wheelRow)")
    assertTrue(Rgb.close(pixels.rgb(aboveColumn, aboveRow), clear), "something above the car")
  }

  private fun render(snapshot: RenderSnapshot = sample.hosted.snapshot): Pixels =
    GpuContext.open().use { gpu ->
      WorldRenderer(gpu, capacity = sample.nodes.size).use { renderer ->
        OffscreenTarget(gpu, size).use { offscreen ->
          gpu.beginFrame()
          val encoder = gpu.frameEncoder()
          renderer.render(snapshot, 1f, offscreen.target, encoder)
          encoder.submit()
          offscreen.read()
        }
      }
    }

  private companion object {
    const val TICKS = 10

    /** Enough ticks for the local client to join and build the scene's nodes. */
    const val CLIENT_TICKS = 30
    const val FLOOR_Z = -0.75f

    /** Where the scene places the imported car: its node's y and z, three times model size. */
    const val CAR_Y = 5.4f
    const val CAR_Z = -0.75f

    /** The middle of the car's body above its origin, at the scene's scale. */
    const val CAR_BODY_HEIGHT = 0.5f

    /** A height above the car's roof, where nothing is drawn. */
    const val CAR_ABOVE = 1.6f

    /** The near front wheel: along the car's length, toward the camera, and its center's height. */
    const val CAR_WHEEL_X = 0.74f
    const val CAR_WHEEL_SIDE = 0.53f
    const val CAR_WHEEL_HEIGHT = 0.03f
    val CLEAR = MeshFrame.DEFAULT_CLEAR
  }
}
