package dev.chieppa.ease.textures.importing

import dev.chieppa.ease.textures.TextureEncoding
import dev.chieppa.ease.textures.bc.Bc5
import dev.chieppa.ease.textures.bc.Bc7
import dev.chieppa.ease.textures.mips.LinearImage
import dev.chieppa.ease.textures.mips.PIXEL_FLOATS
import dev.chieppa.ease.textures.mips.SrgbCurve

private const val BYTE_MAX = 255f
private const val ROUND = 0.5f
private const val HALF_MAX = 65504f
private const val BYTE_MASK = 0xFF
private const val BYTE_BITS = 8

/** Stores one mip level of an image in a [TextureEncoding]. */
internal object LevelEncoder {
  /** [image]'s texels in [encoding]. */
  fun encode(image: LinearImage, encoding: TextureEncoding): ByteArray =
    when (encoding) {
      TextureEncoding.RGBA8_UNORM,
      TextureEncoding.RGBA8_SRGB -> bytes(image, encoding.srgb)
      TextureEncoding.BC7_UNORM,
      TextureEncoding.BC7_SRGB -> Bc7.encode(bytes(image, encoding.srgb), image.width, image.height)
      TextureEncoding.BC5_UNORM -> Bc5.encode(bytes(image, false), image.width, image.height)
      TextureEncoding.RGBA16_FLOAT -> halves(image)
    }

  /** 8 bits a channel; with [srgb], color through the sRGB curve and alpha linear. */
  fun bytes(image: LinearImage, srgb: Boolean): ByteArray {
    val source = image.rgba
    val out = ByteArray(source.size)
    for (index in source.indices) {
      val alpha = index % PIXEL_FLOATS == PIXEL_FLOATS - 1
      out[index] =
        if (srgb && !alpha) SrgbCurve.toCode(source[index]).toByte() else unorm(source[index])
    }
    return out
  }

  private fun unorm(value: Float): Byte =
    (value.coerceIn(0f, 1f) * BYTE_MAX + ROUND).toInt().toByte()

  /** 16-bit floats, little-endian, finite values beyond the half range clamped to it. */
  private fun halves(image: LinearImage): ByteArray {
    val source = image.rgba
    val out = ByteArray(source.size * Short.SIZE_BYTES)
    for (index in source.indices) {
      val half = java.lang.Float.floatToFloat16(source[index].coerceIn(-HALF_MAX, HALF_MAX)).toInt()
      out[index * 2] = (half and BYTE_MASK).toByte()
      out[index * 2 + 1] = ((half shr BYTE_BITS) and BYTE_MASK).toByte()
    }
    return out
  }
}
