package dev.chieppa.ease.gpu

import dev.chieppa.ease.platform.PixelSize
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PixelsTest {
  private val size = PixelSize(2, 1)
  private val bytes = byteArrayOf(0x11, 0x22, 0x33, -1, 0x44, 0x55, 0x66, -1)

  @Test
  fun `RGBA bytes read as red green blue`() {
    val pixels = Pixels(size, TextureFormat.RGBA8Unorm, bytes)

    assertEquals(0x112233, pixels.rgb(0, 0))
    assertEquals(0x445566, pixels.rgb(1, 0))
  }

  @Test
  fun `BGRA bytes read with red and blue swapped back`() {
    val pixels = Pixels(size, TextureFormat.BGRA8Unorm, bytes)

    assertEquals(0x332211, pixels.rgb(0, 0))
  }

  @Test
  fun `a pixel outside the frame is refused`() {
    val pixels = Pixels(size, TextureFormat.RGBA8Unorm, bytes)

    assertFailsWith<IllegalArgumentException> { pixels.rgb(2, 0) }
  }

  @Test
  fun `bytes that do not fill the frame are refused`() {
    assertFailsWith<IllegalArgumentException> {
      Pixels(size, TextureFormat.RGBA8Unorm, ByteArray(4))
    }
  }
}
