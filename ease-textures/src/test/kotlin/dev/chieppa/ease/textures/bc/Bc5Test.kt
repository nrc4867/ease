package dev.chieppa.ease.textures.bc

import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class Bc5Test {
  private fun roundTrip(image: TestImage): ByteArray {
    val blocks = Bc5.encode(image.pixels, image.width, image.height)
    assertEquals((image.width + 3) / 4 * ((image.height + 3) / 4) * 16, blocks.size)
    return Bc5.decode(blocks, image.width, image.height)
  }

  @Test
  fun `red and green come back closely, blue is 0 and alpha 255`() {
    val image = TestImages.of(32, 32) { x, y, c -> intArrayOf(x * 8, 255 - y * 8, 77, 12)[c] }
    val decoded = roundTrip(image)
    assertTrue(TestImages.maxError(image.pixels, decoded, 0..1) <= 3)
    for (pixel in 0 until 32 * 32) {
      assertEquals(0, decoded[pixel * 4 + 2].toInt())
      assertEquals(255, decoded[pixel * 4 + 3].toInt() and 255)
    }
  }

  @Test
  fun `noise stays within BC4's step`() {
    val image = TestImages.noise(16, 16, seed = 3)
    val psnr = TestImages.psnr(image.pixels, roundTrip(image), 0..1)
    println("BC5 noise PSNR: %.2f dB".format(Locale.ROOT, psnr))
    assertTrue(psnr > 24.0, "PSNR $psnr")
  }

  @Test
  fun `values at both extremes use the six-value mode's 0 and 255`() {
    val image = TestImages.of(4, 4) { x, _, c -> if (c > 1) 0 else intArrayOf(0, 100, 110, 255)[x] }
    assertEquals(0, TestImages.maxError(image.pixels, roundTrip(image), 0..1))
  }

  @Test
  fun `a hand-built block decodes with the eight-value interpolation`() {
    val block = ByteArray(16)
    block[0] = 200.toByte()
    block[1] = 60
    var packed = 0L
    for (pixel in 0 until 16) packed = packed or ((pixel % 8).toLong() shl (pixel * 3))
    for (byte in 0 until 6) block[2 + byte] = (packed shr (byte * 8)).toByte()
    val decoded = Bc5.decode(block, 4, 4)
    for (pixel in 0 until 16) {
      val k = pixel % 8
      val expected =
        when (k) {
          0 -> 200
          1 -> 60
          else -> ((8 - k) * 200 + (k - 1) * 60 + 3) / 7
        }
      assertEquals(expected, decoded[pixel * 4].toInt() and 255, "pixel $pixel")
      assertEquals(0, decoded[pixel * 4 + 1].toInt() and 255)
    }
  }
}
