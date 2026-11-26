package dev.chieppa.ease.textures.importing

import dev.chieppa.ease.assets.importing.ImportException
import dev.chieppa.ease.textures.CookedTexture
import dev.chieppa.ease.textures.TextureEncoding
import dev.chieppa.ease.textures.TextureUsage
import dev.chieppa.ease.textures.ktx2.Ktx2Reader
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

class TextureImporterTest {
  private fun png(width: Int, height: Int): ByteArray {
    val image = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
    for (y in 0 until height) {
      for (x in 0 until width) image.setRGB(
        x,
        y,
        (0xFF shl 24) or (x * 16 shl 16) or (y * 16 shl 8) or 0x40,
      )
    }
    return ByteArrayOutputStream().also { ImageIO.write(image, "png", it) }.toByteArray()
  }

  /** A 2 by 2 Radiance HDR file, uncompressed, every pixel the RGBE value given. */
  private fun hdr(rgbe: ByteArray): ByteArray {
    val header = "#?RADIANCE\nFORMAT=32-bit_rle_rgbe\n\n-Y 2 +X 2\n".toByteArray(Charsets.US_ASCII)
    return header + ByteArray(16) { rgbe[it % 4] }
  }

  private fun import(source: ByteArray, settings: String = "{}"): CookedTexture {
    val cooked = TextureImporter.import(source, Json.parseToJsonElement(settings).jsonObject)
    assertEquals(listOf(COOKED_TEXTURE_NAME), cooked.map { it.name })
    return Ktx2Reader.read(cooked.single().bytes)
  }

  @Test
  fun `a color image cooks to sRGB BC7 with every mip level`() {
    val texture = import(png(16, 8))
    assertEquals(TextureEncoding.BC7_SRGB, texture.encoding)
    assertEquals(16 to 8, texture.width to texture.height)
    assertEquals(5, texture.levels.size)
  }

  @Test
  fun `normal maps cook to BC5 and data to linear BC7`() {
    assertEquals(TextureEncoding.BC5_UNORM, import(png(8, 8), """{"usage":"normal"}""").encoding)
    assertEquals(TextureEncoding.BC7_UNORM, import(png(8, 8), """{"usage":"data"}""").encoding)
  }

  @Test
  fun `sides that are not multiples of 4 stay uncompressed`() {
    assertEquals(TextureEncoding.RGBA8_SRGB, import(png(6, 4)).encoding)
  }

  @Test
  fun `settings choose size, storage and mipmaps`() {
    val texture = import(png(16, 16), """{"maxSize":4,"compression":"rgba8","mipmaps":false}""")
    assertEquals(4, texture.width)
    assertEquals(TextureEncoding.RGBA8_SRGB, texture.encoding)
    assertEquals(1, texture.levels.size)
  }

  @Test
  fun `HDR images cook to half floats`() {
    // RGBE (128, 64, 32, 129): mantissas over 256 times 2 to the 1.
    val texture = import(hdr(byteArrayOf(128.toByte(), 64, 32, 129.toByte())))
    assertEquals(TextureEncoding.RGBA16_FLOAT, texture.encoding)
    val level = texture.levels[0]
    val red =
      java.lang.Float.float16ToFloat(
        ((level[1].toInt() and 0xFF shl 8) or (level[0].toInt() and 0xFF)).toShort()
      )
    assertEquals(1.0f, red, 0.01f)
  }

  @Test
  fun `the same source and settings cook the same bytes`() {
    val source = png(32, 32)
    val first = TextureImporter.import(source, JsonObject(emptyMap())).single().bytes
    val second = TextureImporter.import(source, JsonObject(emptyMap())).single().bytes
    assertContentEquals(first, second)
  }

  @Test
  fun `embedded images cook through cookTexture`() {
    val texture = Ktx2Reader.read(cookTexture(png(8, 8), "image/png", TextureUsage.NORMAL))
    assertEquals(TextureEncoding.BC5_UNORM, texture.encoding)
    assertFailsWith<ImportException> { cookTexture(png(8, 8), "image/webp", TextureUsage.NORMAL) }
  }

  @Test
  fun `bytes that are not an image fail the import`() {
    assertFailsWith<ImportException> { import("not an image".toByteArray()) }
  }
}
