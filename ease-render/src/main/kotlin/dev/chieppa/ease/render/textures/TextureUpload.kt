package dev.chieppa.ease.render.textures

import dev.chieppa.ease.gpu.GpuContext
import dev.chieppa.ease.gpu.texture.GpuTexture
import dev.chieppa.ease.gpu.texture.SampledFormat
import dev.chieppa.ease.gpu.texture.SamplerFilter
import dev.chieppa.ease.gpu.texture.SamplerWrap
import dev.chieppa.ease.textures.CookedTexture
import dev.chieppa.ease.textures.TextureEncoding
import dev.chieppa.ease.textures.TextureFilter
import dev.chieppa.ease.textures.TextureWrap
import dev.chieppa.ease.textures.bc.Bc5
import dev.chieppa.ease.textures.bc.Bc7

/**
 * Puts cooked textures on the GPU. Block-compressed levels go up as they are when the device
 * samples block compression; otherwise they are decoded to 8 bits a channel first (sRGB kept), so
 * every device draws every cooked texture, at four times the memory on those without it.
 */
public object TextureUpload {
  /**
   * [texture] as a GPU texture on [context], every level filled; with [decodeBlocks], block
   * compression is decoded first. Close it when done.
   */
  public fun upload(
    context: GpuContext,
    texture: CookedTexture,
    decodeBlocks: Boolean = !context.supportsBlockCompression,
  ): GpuTexture {
    val decode = texture.encoding.compressed && decodeBlocks
    val format = if (decode) decodedFormat(texture.encoding) else formatOf(texture.encoding)
    val gpu =
      GpuTexture(
        context,
        texture.width,
        texture.height,
        format,
        texture.levels.size,
        filterOf(texture.filter),
        wrapOf(texture.wrap),
      )
    texture.levels.forEachIndexed { level, bytes ->
      val width = texture.levelWidth(level)
      val height = texture.levelHeight(level)
      gpu.write(level, if (decode) decoded(texture.encoding, bytes, width, height) else bytes)
    }
    return gpu
  }

  /** The GPU format that samples [encoding] as it is stored. */
  public fun formatOf(encoding: TextureEncoding): SampledFormat =
    when (encoding) {
      TextureEncoding.RGBA8_UNORM -> SampledFormat.RGBA8Unorm
      TextureEncoding.RGBA8_SRGB -> SampledFormat.RGBA8UnormSrgb
      TextureEncoding.RGBA16_FLOAT -> SampledFormat.RGBA16Float
      TextureEncoding.BC5_UNORM -> SampledFormat.BC5RGUnorm
      TextureEncoding.BC7_UNORM -> SampledFormat.BC7RGBAUnorm
      TextureEncoding.BC7_SRGB -> SampledFormat.BC7RGBAUnormSrgb
    }

  private fun decodedFormat(encoding: TextureEncoding): SampledFormat =
    if (encoding.srgb) SampledFormat.RGBA8UnormSrgb else SampledFormat.RGBA8Unorm

  private fun decoded(encoding: TextureEncoding, blocks: ByteArray, width: Int, height: Int) =
    when (encoding) {
      TextureEncoding.BC5_UNORM -> Bc5.decode(blocks, width, height)
      TextureEncoding.BC7_UNORM,
      TextureEncoding.BC7_SRGB -> Bc7.decode(blocks, width, height)
      else -> error("$encoding is not block-compressed")
    }

  private fun filterOf(filter: TextureFilter): SamplerFilter =
    when (filter) {
      TextureFilter.LINEAR -> SamplerFilter.LINEAR
      TextureFilter.NEAREST -> SamplerFilter.NEAREST
    }

  private fun wrapOf(wrap: TextureWrap): SamplerWrap =
    when (wrap) {
      TextureWrap.REPEAT -> SamplerWrap.REPEAT
      TextureWrap.CLAMP -> SamplerWrap.CLAMP
      TextureWrap.MIRROR -> SamplerWrap.MIRROR
    }
}
