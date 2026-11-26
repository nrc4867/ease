package dev.chieppa.ease.platform.images

import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class ImageDecoderTest {
  private fun encoded(format: String, type: Int): ByteArray {
    val image = BufferedImage(3, 2, type)
    image.setRGB(0, 0, 0x7F102030)
    image.setRGB(2, 1, 0xFFFFFFFF.toInt())
    return ByteArrayOutputStream().also { ImageIO.write(image, format, it) }.toByteArray()
  }

  @Test
  fun `a PNG decodes to RGBA bytes, rows from the top`() {
    val image =
      assertIs<DecodedImage.Bytes>(ImageDecoder.decode(encoded("png", BufferedImage.TYPE_INT_ARGB)))
    assertEquals(3 to 2, image.width to image.height)
    assertEquals(listOf(0x10, 0x20, 0x30, 0x7F), image.rgba.take(4).map { it.toInt() and 0xFF })
    assertEquals(-1, image.rgba[(1 * 3 + 2) * 4].toInt())
  }

  @Test
  fun `an image without alpha decodes opaque`() {
    val image =
      assertIs<DecodedImage.Bytes>(ImageDecoder.decode(encoded("bmp", BufferedImage.TYPE_INT_RGB)))
    assertEquals(-1, image.rgba[3].toInt())
  }

  @Test
  fun `an OpenEXR file decodes to linear floats with alpha filled in`() {
    val file =
      ExrFiles.of(2, 2, listOf("R", "G", "B")) { name, x, y ->
        when (name) {
          "R" -> 4f + x
          "G" -> 0.5f * y
          else -> 0.25f
        }
      }
    val image = assertIs<DecodedImage.Floats>(ImageDecoder.decode(file))
    assertEquals(2 to 2, image.width to image.height)
    val last = (1 * 2 + 1) * IMAGE_CHANNELS
    assertEquals(listOf(5f, 0.5f, 0.25f, 1f), image.rgba.slice(last until last + 4))
  }

  @Test
  fun `EXR channels match by name after their layer, ignoring case`() {
    val file =
      ExrFiles.of(1, 1, listOf("beauty.r", "beauty.g", "beauty.b", "beauty.a")) { _, _, _ -> 0.5f }
    val image = assertIs<DecodedImage.Floats>(ImageDecoder.decode(file))
    assertEquals(listOf(0.5f, 0.5f, 0.5f, 0.5f), image.rgba.toList())
  }

  @Test
  fun `an EXR file without color channels is refused`() {
    val file = ExrFiles.of(1, 1, listOf("Z")) { _, _, _ -> 1f }
    assertFailsWith<ImageDecodeException> { ImageDecoder.decode(file) }
  }

  @Test
  fun `a Radiance HDR file decodes to floats`() {
    val header = "#?RADIANCE\nFORMAT=32-bit_rle_rgbe\n\n-Y 1 +X 1\n".toByteArray(Charsets.US_ASCII)
    val image =
      assertIs<DecodedImage.Floats>(ImageDecoder.decode(header + byteArrayOf(-128, 64, 0, -126)))
    assertEquals(listOf(2f, 1f, 0f, 1f), image.rgba.toList())
  }

  @Test
  fun `bytes that are no image are refused with a reason`() {
    assertFailsWith<ImageDecodeException> { ImageDecoder.decode("plain text".toByteArray()) }
    assertFailsWith<ImageDecodeException> { ImageDecoder.decode(ByteArray(0)) }
    assertFailsWith<ImageDecodeException> {
      ImageDecoder.decode(byteArrayOf(0x76, 0x2f, 0x31, 0x01, 2))
    }
  }
}
