package dev.chieppa.ease.gpu

import dev.chieppa.ease.platform.PixelSize
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.test.Test
import kotlin.test.assertTrue

/** The triangle drawn into an offscreen texture and read back, on whatever adapter is present. */
class OffscreenFrameGpuTest {
  private val size = PixelSize(160, 90)

  @Test
  fun `the triangle covers the center and the corners keep the clear color`() {
    val pixels = drawTriangle(TextureFormat.RGBA8Unorm)
    val clear = rgbOf(TrianglePass.CLEAR)
    val corners =
      listOf(0 to 0, size.width - 1 to 0, 0 to size.height - 1, size.width - 1 to size.height - 1)

    for ((x, y) in corners) {
      assertTrue(close(pixels.rgb(x, y), clear), "corner ($x, $y) is ${hex(pixels.rgb(x, y))}")
    }
    val center = pixels.rgb(size.width / 2, size.height / 2)
    assertTrue(!close(center, clear), "the center is the clear color ${hex(center)}")
  }

  @Test
  fun `BGRA and RGBA targets read back the same picture`() {
    val rgba = drawTriangle(TextureFormat.RGBA8Unorm)
    val bgra = drawTriangle(TextureFormat.BGRA8Unorm)

    for (y in 0 until size.height step 9) {
      for (x in 0 until size.width step 16) {
        assertTrue(close(rgba.rgb(x, y), bgra.rgb(x, y)), "pixel ($x, $y) differs")
      }
    }
  }

  private fun drawTriangle(format: TextureFormat): Pixels =
    GpuContext.open().use { gpu ->
      TrianglePass(gpu).use { triangle ->
        OffscreenTarget(gpu, size, format).use { offscreen ->
          val encoder = CommandEncoder.on(gpu)
          triangle.record(encoder, offscreen.target)
          encoder.submit()
          offscreen.read()
        }
      }
    }

  private fun rgbOf(color: Color): Int {
    fun channel(value: Float) = (value * 255).roundToInt()
    return (channel(color.red) shl 16) or (channel(color.green) shl 8) or channel(color.blue)
  }

  /** Whether every channel of [a] and [b] is within one step, as rounding allows. */
  private fun close(a: Int, b: Int): Boolean =
    listOf(16, 8, 0).all { shift -> abs((a shr shift and 0xFF) - (b shr shift and 0xFF)) <= 1 }

  private fun hex(rgb: Int) = "#" + rgb.toString(16).padStart(6, '0')
}
