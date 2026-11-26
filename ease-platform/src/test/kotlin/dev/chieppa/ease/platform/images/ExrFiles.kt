package dev.chieppa.ease.platform.images

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** Writes minimal OpenEXR files for tests: one part, scanlines, no compression, float channels. */
internal object ExrFiles {
  private const val MAGIC = 20000630
  private const val VERSION = 2
  private const val FLOAT_TYPE = 2

  /**
   * A [width] by [height] file whose channels, named in [channels], hold [value] (channel name, x,
   * y) at each pixel.
   */
  fun of(
    width: Int,
    height: Int,
    channels: List<String>,
    value: (String, Int, Int) -> Float,
  ): ByteArray {
    val sorted = channels.sorted()
    val header = ByteArrayOutputStream()
    header.write(le(MAGIC) + le(VERSION))
    attribute(header, "channels", "chlist", channelList(sorted))
    attribute(header, "compression", "compression", byteArrayOf(0))
    val window = le(0) + le(0) + le(width - 1) + le(height - 1)
    attribute(header, "dataWindow", "box2i", window)
    attribute(header, "displayWindow", "box2i", window)
    attribute(header, "lineOrder", "lineOrder", byteArrayOf(0))
    attribute(header, "pixelAspectRatio", "float", lef(1f))
    attribute(header, "screenWindowCenter", "v2f", lef(0f) + lef(0f))
    attribute(header, "screenWindowWidth", "float", lef(1f))
    header.write(0)
    val lineBytes = sorted.size * width * Float.SIZE_BYTES
    val tableEnd = header.size() + height * Long.SIZE_BYTES
    for (y in 0 until height) {
      header.write(lel(tableEnd.toLong() + y * (2L * Int.SIZE_BYTES + lineBytes)))
    }
    for (y in 0 until height) {
      header.write(le(y) + le(lineBytes))
      for (name in sorted) for (x in 0 until width) header.write(lef(value(name, x, y)))
    }
    return header.toByteArray()
  }

  private fun channelList(names: List<String>): ByteArray {
    val out = ByteArrayOutputStream()
    for (name in names) {
      out.write(name.toByteArray(Charsets.US_ASCII) + 0)
      out.write(le(FLOAT_TYPE) + byteArrayOf(0, 0, 0, 0) + le(1) + le(1))
    }
    out.write(0)
    return out.toByteArray()
  }

  private fun attribute(out: ByteArrayOutputStream, name: String, type: String, value: ByteArray) {
    out.write(name.toByteArray(Charsets.US_ASCII) + 0)
    out.write(type.toByteArray(Charsets.US_ASCII) + 0)
    out.write(le(value.size) + value)
  }

  private fun le(value: Int): ByteArray =
    ByteBuffer.allocate(Int.SIZE_BYTES).order(ByteOrder.LITTLE_ENDIAN).putInt(value).array()

  private fun lel(value: Long): ByteArray =
    ByteBuffer.allocate(Long.SIZE_BYTES).order(ByteOrder.LITTLE_ENDIAN).putLong(value).array()

  private fun lef(value: Float): ByteArray =
    ByteBuffer.allocate(Float.SIZE_BYTES).order(ByteOrder.LITTLE_ENDIAN).putFloat(value).array()
}
