package dev.chieppa.ease.gpu.texture

import dev.chieppa.ease.platform.images.TextureSizes.BLOCK_BYTES
import dev.chieppa.ease.platform.images.TextureSizes.BLOCK_SIZE
import dev.chieppa.ease.platform.images.TextureSizes.RGBA16F_BYTES
import dev.chieppa.ease.platform.images.TextureSizes.RGBA8_BYTES
import dev.chieppa.webgpu.ffi.wgpu_h

/**
 * The formats textures are sampled from. Block formats store [blockSize] by [blockSize] texels in
 * [blockBytes] and need [GpuContext.supportsBlockCompression]; the others store one texel in
 * [blockBytes].
 */
public enum class SampledFormat(
  internal val native: Int,
  /** Texels along each side of a block: 4 for block compression, 1 otherwise. */
  public val blockSize: Int,
  /** Bytes per block. */
  public val blockBytes: Int,
) {
  RGBA8Unorm(wgpu_h.WGPUTextureFormat_RGBA8Unorm(), 1, RGBA8_BYTES),
  RGBA8UnormSrgb(wgpu_h.WGPUTextureFormat_RGBA8UnormSrgb(), 1, RGBA8_BYTES),
  RGBA16Float(wgpu_h.WGPUTextureFormat_RGBA16Float(), 1, RGBA16F_BYTES),
  BC5RGUnorm(wgpu_h.WGPUTextureFormat_BC5RGUnorm(), BLOCK_SIZE, BLOCK_BYTES),
  BC7RGBAUnorm(wgpu_h.WGPUTextureFormat_BC7RGBAUnorm(), BLOCK_SIZE, BLOCK_BYTES),
  BC7RGBAUnormSrgb(wgpu_h.WGPUTextureFormat_BC7RGBAUnormSrgb(), BLOCK_SIZE, BLOCK_BYTES);

  /** Whether texels are stored in compressed blocks. */
  public val compressed: Boolean
    get() = blockSize > 1
}
