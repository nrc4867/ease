package dev.chieppa.ease.textures.ktx2

import dev.chieppa.ease.textures.CookedTexture
import dev.chieppa.ease.textures.TextureEncoding
import dev.chieppa.ease.textures.TextureFilter
import dev.chieppa.ease.textures.TextureWrap
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class Ktx2Test {
  private fun texture(encoding: TextureEncoding, width: Int, height: Int, levels: Int) =
    CookedTexture(
      width,
      height,
      encoding,
      List(levels) { level ->
        val bytes =
          encoding.levelBytes(maxOf(1, width shr level), maxOf(1, height shr level)).toInt()
        Random(level).nextBytes(bytes)
      },
      TextureFilter.NEAREST,
      TextureWrap.MIRROR,
    )

  @Test
  fun `every encoding reads back as written`() {
    for (encoding in TextureEncoding.entries) {
      val written = texture(encoding, 16, 8, 5)
      val read = Ktx2Reader.read(Ktx2Writer.write(written))
      assertEquals(encoding, read.encoding)
      assertEquals(16, read.width)
      assertEquals(8, read.height)
      assertEquals(TextureFilter.NEAREST, read.filter)
      assertEquals(TextureWrap.MIRROR, read.wrap)
      written.levels.zip(read.levels).forEach { (a, b) -> assertContentEquals(a, b) }
    }
  }

  @Test
  fun `the header and level index follow the KTX2 layout`() {
    val bytes = Ktx2Writer.write(texture(TextureEncoding.BC7_SRGB, 8, 8, 4))
    val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
    assertContentEquals(Ktx2Layout.IDENTIFIER, bytes.copyOf(12))
    assertEquals(146, buffer.getInt(12)) // VK_FORMAT_BC7_SRGB_BLOCK
    assertEquals(1, buffer.getInt(16)) // typeSize
    assertEquals(4, buffer.getInt(40)) // levelCount
    // Levels are stored smallest first, each aligned to the 16-byte block.
    val offsets = List(4) { buffer.getLong(80 + it * 24) }
    assertEquals(offsets.sortedDescending(), offsets)
    offsets.forEach { assertEquals(0L, it % 16) }
    assertEquals(bytes.size.toLong(), offsets[0] + 64)
  }

  @Test
  fun `writing is deterministic`() {
    val texture = texture(TextureEncoding.RGBA16_FLOAT, 5, 3, 3)
    assertContentEquals(Ktx2Writer.write(texture), Ktx2Writer.write(texture))
  }

  @Test
  fun `damaged files are refused with a reason`() {
    val bytes = Ktx2Writer.write(texture(TextureEncoding.RGBA8_UNORM, 4, 4, 1))
    assertFailsWith<Ktx2Exception> { Ktx2Reader.read(bytes.copyOf(40)) }
    assertFailsWith<Ktx2Exception> { Ktx2Reader.read(ByteArray(100)) }
    val unknownFormat =
      bytes.copyOf().also { ByteBuffer.wrap(it).order(ByteOrder.LITTLE_ENDIAN).putInt(12, 9) }
    assertFailsWith<Ktx2Exception> { Ktx2Reader.read(unknownFormat) }
  }
}
