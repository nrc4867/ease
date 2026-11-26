package dev.chieppa.ease.gpu.texture

import dev.chieppa.ease.gpu.GpuContext
import dev.chieppa.ease.gpu.ffi.Flags
import dev.chieppa.ease.gpu.ffi.createTexture
import dev.chieppa.ease.gpu.ffi.writeTextureLevel
import dev.chieppa.ease.platform.PixelSize
import dev.chieppa.ease.platform.images.TextureSizes
import dev.chieppa.webgpu.ffi.wgpu_h
import java.lang.foreign.MemorySegment

/**
 * A texture shaders sample: [width] by [height] texels in [format] with [mipLevels] levels, each
 * filled once with [write], read through a sampler with [filter] and [wrap]. A block format needs
 * [GpuContext.supportsBlockCompression]. Made while loading, never while recording a frame.
 */
@Suppress("LongParameterList") // A texture's shape and how it is sampled.
public class GpuTexture(
  private val context: GpuContext,
  public val width: Int,
  public val height: Int,
  public val format: SampledFormat,
  public val mipLevels: Int = 1,
  public val filter: SamplerFilter = SamplerFilter.LINEAR,
  public val wrap: SamplerWrap = SamplerWrap.REPEAT,
) : AutoCloseable {
  init {
    require(width > 0 && height > 0) { "A texture has texels; got $width x $height" }
    val most = TextureSizes.levelCount(width, height)
    require(mipLevels in 1..most) {
      "A $width x $height texture has 1 to $most levels, not $mipLevels"
    }
    require(!format.compressed || context.supportsBlockCompression) {
      "$format needs block compression, which this device does not support"
    }
    require(TextureSizes.blockAligned(width, height, format.blockSize)) {
      "A $format texture's sides are multiples of ${format.blockSize}, not $width x $height"
    }
  }

  private val texture =
    createTexture(
      context.device,
      PixelSize(width, height),
      format.native,
      Flags.TEXTURE_BINDING or Flags.TEXTURE_COPY_DST,
      mipLevels,
    )

  internal val view: MemorySegment = wgpu_h.wgpuTextureCreateView(texture, MemorySegment.NULL)

  internal val sampler: MemorySegment
    get() = context.samplers.of(filter, wrap)

  /** Bytes mip level [level] holds: whole blocks, rows from the top. */
  public fun levelBytes(level: Int): Int =
    Math.toIntExact(
      TextureSizes.levelBytes(
        levelWidth(level),
        levelHeight(level),
        format.blockSize,
        format.blockBytes,
      )
    )

  /** Fills mip level [level] with [bytes], [levelBytes] of them. */
  public fun write(level: Int, bytes: ByteArray) {
    require(level in 0 until mipLevels) { "No level $level among $mipLevels" }
    require(bytes.size == levelBytes(level)) {
      "Level $level takes ${levelBytes(level)} bytes, not ${bytes.size}"
    }
    val block = format.blockSize
    val across = TextureSizes.blocks(levelWidth(level), block)
    val rows = TextureSizes.blocks(levelHeight(level), block)
    // A block format copies whole blocks, so the extent is rounded up to them.
    val extent = PixelSize(across * block, rows * block)
    val size = if (format.compressed) extent else PixelSize(levelWidth(level), levelHeight(level))
    writeTextureLevel(
      context.queue,
      texture,
      level,
      size,
      bytes,
      across * format.blockBytes,
      rows,
    )
  }

  /** The width of mip level [level]. */
  public fun levelWidth(level: Int): Int = TextureSizes.levelSide(width, level)

  /** The height of mip level [level]. */
  public fun levelHeight(level: Int): Int = TextureSizes.levelSide(height, level)

  override fun close() {
    wgpu_h.wgpuTextureViewRelease(view)
    wgpu_h.wgpuTextureRelease(texture)
  }
}
