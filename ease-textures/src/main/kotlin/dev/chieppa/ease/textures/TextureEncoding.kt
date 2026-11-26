package dev.chieppa.ease.textures

import dev.chieppa.ease.platform.images.TextureSizes
import dev.chieppa.ease.platform.images.TextureSizes.BLOCK_BYTES
import dev.chieppa.ease.platform.images.TextureSizes.BLOCK_SIZE
import dev.chieppa.ease.platform.images.TextureSizes.RGBA16F_BYTES
import dev.chieppa.ease.platform.images.TextureSizes.RGBA8_BYTES

private const val VK_R8G8B8A8_UNORM = 37
private const val VK_R8G8B8A8_SRGB = 43
private const val VK_R16G16B16A16_SFLOAT = 97
private const val VK_BC5_UNORM_BLOCK = 141
private const val VK_BC7_UNORM_BLOCK = 145
private const val VK_BC7_SRGB_BLOCK = 146

/**
 * How a cooked texture's texels are stored, with the Vulkan format number KTX2 names it by. Block
 * formats store [blockSize] by [blockSize] texels in [blockBytes]; the others store one texel in
 * [blockBytes].
 */
public enum class TextureEncoding(
  /** The `VkFormat` value KTX2 files record. */
  public val vkFormat: Int,
  /** Texels along each side of a block: 4 for block compression, 1 otherwise. */
  public val blockSize: Int,
  /** Bytes per block. */
  public val blockBytes: Int,
  /** Whether samplers decode the stored values from sRGB to linear. */
  public val srgb: Boolean,
) {
  /** Four 8-bit linear channels. */
  RGBA8_UNORM(VK_R8G8B8A8_UNORM, 1, RGBA8_BYTES, false),

  /** Four 8-bit channels, color in sRGB, alpha linear. */
  RGBA8_SRGB(VK_R8G8B8A8_SRGB, 1, RGBA8_BYTES, true),

  /** Four 16-bit floats, for high dynamic range. */
  RGBA16_FLOAT(VK_R16G16B16A16_SFLOAT, 1, RGBA16F_BYTES, false),

  /** BC5: two 8-bit-ish channels (red and green), for normal maps. */
  BC5_UNORM(VK_BC5_UNORM_BLOCK, BLOCK_SIZE, BLOCK_BYTES, false),

  /** BC7: color and alpha, linear. */
  BC7_UNORM(VK_BC7_UNORM_BLOCK, BLOCK_SIZE, BLOCK_BYTES, false),

  /** BC7: color in sRGB and linear alpha. */
  BC7_SRGB(VK_BC7_SRGB_BLOCK, BLOCK_SIZE, BLOCK_BYTES, true);

  /** Whether texels are stored in compressed blocks. */
  public val compressed: Boolean
    get() = blockSize > 1

  /** Bytes one mip level of [width] by [height] texels takes; partial blocks count whole. */
  public fun levelBytes(width: Int, height: Int): Long =
    TextureSizes.levelBytes(width, height, blockSize, blockBytes)

  public companion object {
    /** The encoding KTX2 names [vkFormat], or null when Ease does not read it. */
    public fun ofVkFormat(vkFormat: Int): TextureEncoding? = entries.firstOrNull {
      it.vkFormat == vkFormat
    }
  }
}
