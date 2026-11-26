package dev.chieppa.ease.textures.mips

import dev.chieppa.ease.textures.TextureUsage
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MipChainTest {
  private fun image(width: Int, height: Int, pixel: (Int, Int) -> FloatArray): LinearImage {
    val rgba = FloatArray(width * height * PIXEL_FLOATS)
    for (y in 0 until height) {
      for (x in 0 until width) pixel(x, y).copyInto(rgba, (y * width + x) * PIXEL_FLOATS)
    }
    return LinearImage(width, height, rgba)
  }

  @Test
  fun `a full chain halves down to one pixel`() {
    val levels =
      MipChain.build(
        image(12, 5) { _, _ -> floatArrayOf(1f, 1f, 1f, 1f) },
        TextureUsage.DATA_LINEAR,
        true,
      )
    assertEquals(listOf(12 to 5, 6 to 2, 3 to 1, 1 to 1), levels.map { it.width to it.height })
  }

  @Test
  fun `the box filter keeps the average of an odd-sized image`() {
    val source = image(5, 3) { x, y -> floatArrayOf(x / 4f, y / 2f, 0.25f, 1f) }
    val small = AreaResize.resize(source, 2, 1, TextureUsage.DATA_LINEAR)
    val mean = (0 until 2).map { small.rgba[it * PIXEL_FLOATS] }.average()
    assertTrue(abs(mean - 0.5) < 1e-6, "mean red $mean")
    assertEquals(0.5f, small.rgba[1], 1e-6f)
  }

  @Test
  fun `transparent pixels do not bleed their color`() {
    val source =
      image(2, 1) { x, _ ->
        if (x == 0) floatArrayOf(1f, 0f, 0f, 1f) else floatArrayOf(0f, 0f, 1f, 0f)
      }
    val pixel = AreaResize.half(source, TextureUsage.COLOR_SRGB).rgba
    assertEquals(1f, pixel[0])
    assertEquals(0f, pixel[2])
    assertEquals(0.5f, pixel[3])
  }

  @Test
  fun `normals average to a unit vector`() {
    val source =
      image(2, 1) { x, _ ->
        if (x == 0) floatArrayOf(1f, 0.5f, 0.5f, 1f) else floatArrayOf(0.5f, 0.5f, 1f, 1f)
      }
    val pixel = AreaResize.half(source, TextureUsage.NORMAL).rgba
    val x = pixel[0] * 2 - 1
    val z = pixel[2] * 2 - 1
    assertEquals(1f, x * x + z * z, 1e-5f)
    assertEquals(x, z, 1e-5f)
  }

  @Test
  fun `fitting halves until both sides fit`() {
    val fitted =
      MipChain.fit(
        image(40, 10) { _, _ -> floatArrayOf(0f, 0f, 0f, 1f) },
        16,
        TextureUsage.DATA_LINEAR,
      )
    assertEquals(10 to 2, fitted.width to fitted.height)
  }

  @Test
  fun `the sRGB curve round-trips every code`() {
    for (code in 0..255) assertEquals(code, SrgbCurve.toCode(SrgbCurve.toLinear(code)))
    assertEquals(0, SrgbCurve.toCode(-1f))
    assertEquals(255, SrgbCurve.toCode(2f))
  }
}
