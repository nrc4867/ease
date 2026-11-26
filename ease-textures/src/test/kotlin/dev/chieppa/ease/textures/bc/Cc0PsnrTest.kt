package dev.chieppa.ease.textures.bc

import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Compression quality on real textures (CC0, see `cc0/sources.txt`), against the uncompressed
 * image.
 */
class Cc0PsnrTest {
  private fun bc7(name: String, floor: Double) {
    val image = TestImages.cc0(name)
    val started = System.nanoTime()
    val blocks = Bc7.encode(image.pixels, image.width, image.height)
    val millis = (System.nanoTime() - started) / 1_000_000
    val psnr = TestImages.psnr(image.pixels, Bc7.decode(blocks, image.width, image.height), 0..2)
    println(
      "BC7 $name ${image.width}x${image.height}: %.2f dB, encoded in $millis ms"
        .format(Locale.ROOT, psnr)
    )
    assertTrue(psnr >= floor, "$name: PSNR $psnr below $floor")
  }

  @Test fun `BC7 on rocks`() = bc7("aerial_rocks_02_diff.png", 39.0)

  @Test fun `BC7 on bark`() = bc7("bark_brown_02_diff.png", 42.5)

  @Test fun `BC7 on bricks`() = bc7("red_brick_03_diff.png", 42.0)

  @Test
  fun `BC5 on a normal map`() {
    val image = TestImages.cc0("red_brick_03_nor_gl.png")
    val decoded =
      Bc5.decode(Bc5.encode(image.pixels, image.width, image.height), image.width, image.height)
    val psnr = TestImages.psnr(image.pixels, decoded, 0..1)
    println("BC5 red_brick_03_nor_gl.png: %.2f dB".format(Locale.ROOT, psnr))
    assertTrue(psnr >= 45.0, "PSNR $psnr")
  }
}
