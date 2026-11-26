package dev.chieppa.ease.textures.ktx2

import dev.chieppa.ease.textures.TextureEncoding
import java.nio.ByteBuffer
import java.nio.ByteOrder

private const val MODEL_RGBSDA = 1
private const val MODEL_BC5 = 132
private const val MODEL_BC7 = 134
private const val PRIMARIES_BT709 = 1
private const val TRANSFER_LINEAR = 1
private const val TRANSFER_SRGB = 2
private const val VERSION = 2
private const val BLOCK_HEADER_BYTES = 24
private const val SAMPLE_BYTES = 16
private const val CHANNEL_ALPHA = 15
private const val QUALIFIER_LINEAR = 0x10
private const val QUALIFIER_FLOAT_SIGNED = 0xC0
private const val FLOAT_MINUS_ONE = 0xBF800000L
private const val FLOAT_ONE = 0x3F800000L
private const val ALL_BITS = 0xFFFFFFFFL
private const val BYTE_MAX = 255L
private const val BLOCK_DIMENSION = 3
private const val BITS_PER_BYTE = 8
private const val LENGTH_SHIFT = 16
private const val CHANNEL_SHIFT = 24
private const val SIZE_SHIFT = 16
private const val PRIMARIES_SHIFT = 8
private const val TRANSFER_SHIFT = 16

/** One channel of a data format descriptor: where its bits are and what they span. */
private class Sample(
  val channel: Int,
  val bitOffset: Int,
  val bits: Int,
  val qualifiers: Int,
  val lower: Long,
  val upper: Long,
)

/**
 * The KTX2 data format descriptor of an encoding: the Khronos basic descriptor block, with its
 * total size in front, as KTX2 stores it.
 */
internal object DataFormatDescriptor {
  /** The descriptor bytes for [encoding]. */
  fun of(encoding: TextureEncoding): ByteArray {
    val samples = samples(encoding)
    val blockBytes = BLOCK_HEADER_BYTES + SAMPLE_BYTES * samples.size
    val buffer = ByteBuffer.allocate(Int.SIZE_BYTES + blockBytes).order(ByteOrder.LITTLE_ENDIAN)
    buffer.putInt(Int.SIZE_BYTES + blockBytes)
    buffer.putInt(0) // vendor Khronos, basic descriptor type
    buffer.putInt(VERSION or (blockBytes shl SIZE_SHIFT))
    val transfer = if (encoding.srgb) TRANSFER_SRGB else TRANSFER_LINEAR
    buffer.putInt(
      model(encoding) or (PRIMARIES_BT709 shl PRIMARIES_SHIFT) or (transfer shl TRANSFER_SHIFT)
    )
    val dimension = if (encoding.compressed) BLOCK_DIMENSION else 0
    val dimensions = byteArrayOf(dimension.toByte(), dimension.toByte(), 0, 0)
    buffer.put(dimensions)
    buffer.putInt(encoding.blockBytes)
    buffer.putInt(0)
    for (sample in samples) {
      buffer.putInt(
        sample.bitOffset or
          ((sample.bits - 1) shl LENGTH_SHIFT) or
          ((sample.channel or sample.qualifiers) shl CHANNEL_SHIFT)
      )
      buffer.putInt(0)
      buffer.putInt(sample.lower.toInt())
      buffer.putInt(sample.upper.toInt())
    }
    return buffer.array()
  }

  private fun model(encoding: TextureEncoding): Int =
    when (encoding) {
      TextureEncoding.BC5_UNORM -> MODEL_BC5
      TextureEncoding.BC7_UNORM,
      TextureEncoding.BC7_SRGB -> MODEL_BC7
      else -> MODEL_RGBSDA
    }

  private fun samples(encoding: TextureEncoding): List<Sample> =
    when (encoding) {
      TextureEncoding.RGBA8_UNORM,
      TextureEncoding.RGBA8_SRGB -> channels(BITS_PER_BYTE, 0, 0, BYTE_MAX, encoding.srgb)
      TextureEncoding.RGBA16_FLOAT ->
        channels(Short.SIZE_BITS, QUALIFIER_FLOAT_SIGNED, FLOAT_MINUS_ONE, FLOAT_ONE, false)
      TextureEncoding.BC5_UNORM ->
        listOf(
          Sample(0, 0, Long.SIZE_BITS, 0, 0, ALL_BITS),
          Sample(1, Long.SIZE_BITS, Long.SIZE_BITS, 0, 0, ALL_BITS),
        )
      TextureEncoding.BC7_UNORM,
      TextureEncoding.BC7_SRGB -> listOf(Sample(0, 0, 2 * Long.SIZE_BITS, 0, 0, ALL_BITS))
    }

  /** Red, green, blue and alpha of [bits] each; alpha is marked linear under an sRGB transfer. */
  private fun channels(
    bits: Int,
    qualifiers: Int,
    lower: Long,
    upper: Long,
    srgb: Boolean,
  ): List<Sample> =
    listOf(0, 1, 2, CHANNEL_ALPHA).mapIndexed { index, channel ->
      val alphaLinear = if (srgb && channel == CHANNEL_ALPHA) QUALIFIER_LINEAR else 0
      Sample(channel, index * bits, bits, qualifiers or alphaLinear, lower, upper)
    }
}
