package dev.chieppa.ease.textures.importing

import dev.chieppa.ease.platform.images.DecodedImage
import dev.chieppa.ease.platform.images.TextureSizes
import dev.chieppa.ease.platform.images.TextureSizes.BLOCK_SIZE
import dev.chieppa.ease.textures.CookedTexture
import dev.chieppa.ease.textures.TextureEncoding
import dev.chieppa.ease.textures.TextureUsage
import dev.chieppa.ease.textures.mips.LinearImage
import dev.chieppa.ease.textures.mips.MipChain
import dev.chieppa.ease.textures.mips.SrgbCurve

private const val BYTE_MASK = 0xFF
private const val BYTE_MAX = 255f

/**
 * Turns a decoded image into a [CookedTexture] under [TextureSettings]: into linear values (sRGB
 * color decoded to linear light), halved to fit the largest size, mipmapped with a box filter, and
 * each level stored in the chosen encoding. Block compression needs both sides of the full-size
 * image to be multiples of 4; an image that is not is stored uncompressed instead. Every step uses
 * whole-number weights or strict floating point, so the bytes are the same on every machine.
 */
public object TextureCooker {
  /** [image] cooked under [settings]. */
  public fun cook(image: DecodedImage, settings: TextureSettings): CookedTexture {
    val usage = settings.usage
    val linear = MipChain.fit(linearOf(image, usage), settings.maxSize, usage)
    val encoding = encodingFor(image, linear, settings)
    val levels = MipChain.build(linear, usage, settings.mipmaps)
    return CookedTexture(
      linear.width,
      linear.height,
      encoding,
      levels.map { LevelEncoder.encode(it, encoding) },
      settings.filter,
      settings.wrap,
    )
  }

  /** The encoding [settings] ask for, given the source and the size it was fitted to. */
  internal fun encodingFor(
    source: DecodedImage,
    fitted: LinearImage,
    settings: TextureSettings,
  ): TextureEncoding {
    val srgb = settings.usage == TextureUsage.COLOR_SRGB
    val compression = compressionFor(source, settings)
    val blockAligned = TextureSizes.blockAligned(fitted.width, fitted.height, BLOCK_SIZE)
    val uncompressed = if (srgb) TextureEncoding.RGBA8_SRGB else TextureEncoding.RGBA8_UNORM
    return when (compression) {
      TextureCompression.AUTO,
      TextureCompression.BC7 ->
        when {
          !blockAligned -> uncompressed
          srgb -> TextureEncoding.BC7_SRGB
          else -> TextureEncoding.BC7_UNORM
        }
      TextureCompression.BC5 -> if (blockAligned) TextureEncoding.BC5_UNORM else uncompressed
      TextureCompression.RGBA8 -> uncompressed
      TextureCompression.RGBA16F -> TextureEncoding.RGBA16_FLOAT
    }
  }

  /** The compression [settings] name, with `auto` resolved for [source]. */
  private fun compressionFor(source: DecodedImage, settings: TextureSettings): TextureCompression =
    when {
      settings.compression != TextureCompression.AUTO -> settings.compression
      source is DecodedImage.Floats -> TextureCompression.RGBA16F
      settings.usage == TextureUsage.NORMAL -> TextureCompression.BC5
      else -> TextureCompression.BC7
    }

  /** [image] as linear floats: sRGB color through the curve, everything else divided by 255. */
  internal fun linearOf(image: DecodedImage, usage: TextureUsage): LinearImage =
    when (image) {
      is DecodedImage.Floats -> LinearImage(image.width, image.height, image.rgba.copyOf())
      is DecodedImage.Bytes -> {
        val bytes = image.rgba
        val srgb = usage == TextureUsage.COLOR_SRGB
        val out = FloatArray(bytes.size)
        for (index in bytes.indices) {
          val code = bytes[index].toInt() and BYTE_MASK
          val alpha = index % 4 == 3
          out[index] = if (srgb && !alpha) SrgbCurve.toLinear(code) else code / BYTE_MAX
        }
        LinearImage(image.width, image.height, out)
      }
    }
}
