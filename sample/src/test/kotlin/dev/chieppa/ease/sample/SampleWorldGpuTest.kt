package dev.chieppa.ease.sample

import dev.chieppa.ease.gpu.Color
import dev.chieppa.ease.gpu.CommandEncoder
import dev.chieppa.ease.gpu.GpuContext
import dev.chieppa.ease.gpu.InstancedQuadPass
import dev.chieppa.ease.gpu.OffscreenTarget
import dev.chieppa.ease.gpu.Pixels
import dev.chieppa.ease.platform.PixelSize
import dev.chieppa.ease.render.WorldRenderer
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.test.Test
import kotlin.test.assertTrue

/** The sample world rendered offscreen by the same renderer the player uses, then probed. */
class SampleWorldGpuTest {
  private val size = PixelSize(320, 180)
  private val sample = SampleWorld()
  private val camera = sample.module.camera()
  private val pixelsPerUnit = size.height / camera.visibleHeight

  @Test
  fun `every square shows its color at its projected center and the corners stay clear`() {
    sample.step(TICKS)

    val pixels = render()

    sample.entities.zip(SampleGame.SQUARES).forEach { (entity, square) ->
      val column = pixelX(sample.x(entity))
      val row = pixelY(sample.y(entity))
      val found = pixels.rgb(column, row)
      assertTrue(close(found, square.rgb), "square at ($column, $row) is ${hex(found)}")
    }
    val clear = rgbOf(InstancedQuadPass.DEFAULT_CLEAR)
    val corners =
      listOf(0 to 0, size.width - 1 to 0, 0 to size.height - 1, size.width - 1 to size.height - 1)
    for ((column, row) in corners) {
      assertTrue(close(pixels.rgb(column, row), clear), "corner ($column, $row) is not clear")
    }
  }

  private fun render(): Pixels =
    GpuContext.open().use { gpu ->
      WorldRenderer(gpu, capacity = SampleGame.SQUARES.size).use { renderer ->
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

  private fun rgbOf(color: Color): Int {
    fun channel(value: Float) = (value * CHANNEL_MAX).roundToInt()
    return (channel(color.red) shl RED_SHIFT) or
      (channel(color.green) shl GREEN_SHIFT) or
      channel(color.blue)
  }

  /** Whether every channel of [a] and [b] is within one step, as rounding allows. */
  private fun close(a: Int, b: Int): Boolean =
    listOf(RED_SHIFT, GREEN_SHIFT, 0).all { shift ->
      abs((a shr shift and CHANNEL) - (b shr shift and CHANNEL)) <= 1
    }

  private fun hex(rgb: Int) = "#" + rgb.toString(HEX).padStart(HEX_DIGITS, '0')

  private companion object {
    const val TICKS = 10
    const val RED_SHIFT = 16
    const val GREEN_SHIFT = 8
    const val CHANNEL = 0xFF
    const val CHANNEL_MAX = 255f
    const val HEX = 16
    const val HEX_DIGITS = 6
  }
}
