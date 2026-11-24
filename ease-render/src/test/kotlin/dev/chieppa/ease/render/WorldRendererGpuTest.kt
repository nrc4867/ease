package dev.chieppa.ease.render

import dev.chieppa.ease.ecs.LocalTransform
import dev.chieppa.ease.ecs.Phase
import dev.chieppa.ease.ecs.phasedWorld
import dev.chieppa.ease.gpu.CommandEncoder
import dev.chieppa.ease.gpu.GpuContext
import dev.chieppa.ease.gpu.OffscreenTarget
import dev.chieppa.ease.gpu.Pixels
import dev.chieppa.ease.platform.PixelSize
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * A quad at x = -2 on one tick and x = 2 on the next, drawn at alpha 0.5: it shows at the center
 * and not at either end.
 */
class WorldRendererGpuTest {
  private val size = PixelSize(200, 100)
  private val camera = Camera2D(visibleHeight = 10f)

  @Test
  fun `a quad is drawn between its previous and current tick by alpha`() {
    val snapshot = RenderSnapshot()
    val world = phasedWorld { add(Phase.EXTRACT, ExtractSystem(snapshot)) }
    val entity = world.entity {
      it += LocalTransform().apply { x = -2f }
      it += Quad.of(size = 1f, red = 1f, green = 1f, blue = 0f)
    }
    world.update(DELTA)
    with(world) { entity[LocalTransform].x = 2f }
    world.update(DELTA)

    val pixels = draw(snapshot, alpha = 0.5f)

    assertTrue(yellow(pixels.rgb(size.width / 2, size.height / 2)), "the quad is not centered")
    assertTrue(!yellow(pixels.rgb(pixelX(-2f), size.height / 2)), "the quad is at its previous x")
    assertTrue(!yellow(pixels.rgb(pixelX(2f), size.height / 2)), "the quad is at its current x")
  }

  private fun draw(snapshot: RenderSnapshot, alpha: Float): Pixels =
    GpuContext.open().use { gpu ->
      WorldRenderer(gpu, capacity = 4).use { renderer ->
        OffscreenTarget(gpu, size).use { offscreen ->
          val encoder = CommandEncoder.on(gpu)
          renderer.render(snapshot, alpha, camera, offscreen.target, encoder)
          encoder.submit()
          offscreen.read()
        }
      }
    }

  /** The pixel column of world [x]: 10 units span 100 rows, so 10 pixels a unit. */
  private fun pixelX(x: Float): Int = size.width / 2 + (x * PIXELS_PER_UNIT).toInt()

  private fun yellow(rgb: Int): Boolean =
    abs((rgb shr 16 and 0xFF) - 0xFF) <= 1 &&
      abs((rgb shr 8 and 0xFF) - 0xFF) <= 1 &&
      (rgb and 0xFF) <= 1

  private companion object {
    const val DELTA = 1f / 60f
    const val PIXELS_PER_UNIT = 10
  }
}
