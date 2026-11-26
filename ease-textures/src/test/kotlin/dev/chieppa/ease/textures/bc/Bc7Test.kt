package dev.chieppa.ease.textures.bc

import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class Bc7Test {
  private fun roundTrip(image: TestImage): ByteArray {
    val blocks = Bc7.encode(image.pixels, image.width, image.height)
    val blocksAcross = (image.width + 3) / 4
    val blocksDown = (image.height + 3) / 4
    assertEquals(blocksAcross * blocksDown * 16, blocks.size)
    return Bc7.decode(blocks, image.width, image.height)
  }

  @Test
  fun `a flat color comes back within one step`() {
    val image = TestImages.of(8, 8) { _, _, c -> intArrayOf(200, 37, 91, 255)[c] }
    assertTrue(TestImages.maxError(image.pixels, roundTrip(image)) <= 1)
  }

  @Test
  fun `smooth gradients come back closely`() {
    val image = TestImages.of(64, 64) { x, y, c -> intArrayOf(x * 4, y * 4, (x + y) * 2, 255)[c] }
    val psnr = TestImages.psnr(image.pixels, roundTrip(image))
    println("BC7 gradient PSNR: %.2f dB".format(Locale.ROOT, psnr))
    assertTrue(psnr > 45.0, "PSNR $psnr")
  }

  @Test
  fun `noise survives as well as BC7 allows`() {
    val image = TestImages.noise(32, 32, seed = 7)
    val psnr = TestImages.psnr(image.pixels, roundTrip(image))
    println("BC7 noise PSNR: %.2f dB".format(Locale.ROOT, psnr))
    assertTrue(psnr > 18.0, "PSNR $psnr")
  }

  @Test
  fun `an alpha gradient over one color keeps its alpha`() {
    val image = TestImages.of(32, 32) { x, y, c -> intArrayOf(90, 160, 40, (x + y) * 4)[c] }
    val decoded = roundTrip(image)
    val alpha = TestImages.psnr(image.pixels, decoded, 3..3)
    println("BC7 alpha gradient PSNR: %.2f dB".format(Locale.ROOT, alpha))
    assertTrue(alpha > 40.0, "alpha PSNR $alpha")
  }

  @Test
  fun `color and alpha that vary apart share the error`() {
    val image = TestImages.of(32, 32) { x, y, c -> intArrayOf(x * 8, 128, y * 8, (x + y) * 4)[c] }
    val decoded = roundTrip(image)
    val alpha = TestImages.psnr(image.pixels, decoded, 3..3)
    val all = TestImages.psnr(image.pixels, decoded)
    println(
      "BC7 separate gradients PSNR: alpha %.2f dB, all %.2f dB".format(Locale.ROOT, alpha, all)
    )
    assertTrue(alpha > 33.0, "alpha PSNR $alpha")
    assertTrue(all > 39.0, "PSNR $all")
  }

  @Test
  fun `sharp alpha edges keep their alpha`() {
    val image =
      TestImages.of(16, 16) { x, y, c ->
        if (c == 3) (if (x > y) 255 else 0) else (x * 16 + c * 40) % 256
      }
    val decoded = roundTrip(image)
    assertTrue(TestImages.maxError(image.pixels, decoded, 3..3) <= 8)
  }

  @Test
  fun `odd sizes encode whole blocks and decode only the image`() {
    for ((width, height) in listOf(1 to 1, 5 to 3, 3 to 7)) {
      val image =
        TestImages.of(width, height) { x, y, c -> intArrayOf(x * 40, x * 20 + y, 90, 255)[c] }
      val decoded = roundTrip(image)
      assertEquals(width * height * 4, decoded.size)
      assertTrue(TestImages.maxError(image.pixels, decoded) <= 4, "${width}x$height")
    }
  }

  @Test
  fun `the reserved mode decodes as transparent black`() {
    val decoded = Bc7.decode(ByteArray(16), 4, 4)
    assertContentEquals(ByteArray(64), decoded)
  }
}
