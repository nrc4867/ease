package dev.chieppa.ease.textures.ktx2

import dev.chieppa.ease.textures.CookedTexture
import dev.chieppa.ease.textures.TextureEncoding
import java.nio.ByteBuffer
import java.nio.ByteOrder

private const val HALF_BYTES = 2
private const val WORD = 4

/**
 * Writes a [CookedTexture] as a KTX2 file: one 2D image with every mip level, no supercompression,
 * its data format descriptor, and key-value data naming the writer and the sampler's filter and
 * wrap. The same texture always gives the same bytes.
 */
public object Ktx2Writer {
  /** The KTX2 bytes of [texture]. */
  public fun write(texture: CookedTexture): ByteArray {
    val encoding = texture.encoding
    val levelCount = texture.levels.size
    val dfd = DataFormatDescriptor.of(encoding)
    val kvd = keyValues(texture)
    val dfdAt = Ktx2Layout.LEVEL_INDEX_AT + levelCount * Ktx2Layout.LEVEL_ENTRY_BYTES
    val kvdAt = dfdAt + dfd.size
    val alignment = levelAlignment(encoding)
    // Levels are stored smallest first, each at a multiple of the alignment.
    val offsets = LongArray(levelCount)
    var end = (kvdAt + kvd.size).toLong()
    for (level in levelCount - 1 downTo 0) {
      end = align(end, alignment)
      offsets[level] = end
      end += texture.levels[level].size
    }
    val buffer = ByteBuffer.allocate(Math.toIntExact(end)).order(ByteOrder.LITTLE_ENDIAN)
    buffer.put(Ktx2Layout.IDENTIFIER)
    buffer.putInt(encoding.vkFormat)
    buffer.putInt(typeSize(encoding))
    buffer.putInt(texture.width).putInt(texture.height)
    buffer.putInt(0).putInt(0).putInt(1) // depth, layers, faces
    buffer.putInt(levelCount)
    buffer.putInt(0) // no supercompression
    buffer.putInt(dfdAt).putInt(dfd.size)
    buffer.putInt(kvdAt).putInt(kvd.size)
    buffer.putLong(0).putLong(0) // no supercompression global data
    for (level in 0 until levelCount) {
      val bytes = texture.levels[level].size.toLong()
      buffer.putLong(offsets[level]).putLong(bytes).putLong(bytes)
    }
    buffer.put(dfd).put(kvd)
    for (level in 0 until levelCount) buffer.put(offsets[level].toInt(), texture.levels[level])
    return buffer.array()
  }

  private fun keyValues(texture: CookedTexture): ByteArray {
    val entries =
      sortedMapOf(
        Ktx2Layout.WRITER_KEY to Ktx2Layout.WRITER,
        Ktx2Layout.FILTER_KEY to texture.filter.name.lowercase(),
        Ktx2Layout.WRAP_KEY to texture.wrap.name.lowercase(),
      )
    val encoded = entries.map { (key, value) ->
      "$key\u0000$value\u0000".toByteArray(Charsets.UTF_8)
    }
    val size = encoded.sumOf { Int.SIZE_BYTES + align(it.size.toLong(), WORD).toInt() }
    val buffer = ByteBuffer.allocate(size).order(ByteOrder.LITTLE_ENDIAN)
    for (entry in encoded) {
      buffer.putInt(entry.size).put(entry)
      buffer.position(align(buffer.position().toLong(), WORD).toInt())
    }
    return buffer.array()
  }

  /** KTX2's typeSize: the size of the data type a texel's channels use, 1 for block formats. */
  private fun typeSize(encoding: TextureEncoding): Int =
    if (encoding == TextureEncoding.RGBA16_FLOAT) HALF_BYTES else 1

  /** KTX2's mip padding: the least common multiple of the block size and 4. */
  private fun levelAlignment(encoding: TextureEncoding): Int {
    val block = encoding.blockBytes
    return if (block % WORD == 0) block else block * WORD
  }

  private fun align(value: Long, alignment: Int): Long =
    (value + alignment - 1) / alignment * alignment
}
