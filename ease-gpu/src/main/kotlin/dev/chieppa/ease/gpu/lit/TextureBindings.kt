package dev.chieppa.ease.gpu.lit

import dev.chieppa.ease.gpu.GpuContext
import dev.chieppa.ease.gpu.ffi.Flags
import dev.chieppa.ease.gpu.ffi.LayoutEntry
import dev.chieppa.ease.gpu.ffi.createBindGroupLayout
import dev.chieppa.ease.gpu.texture.GpuTexture
import dev.chieppa.ease.gpu.texture.SampledFormat
import dev.chieppa.webgpu.ffi.wgpu_h
import java.lang.foreign.MemorySegment

private const val FULL = 0xFF.toByte()

/** A half float's bytes, little-endian: 0.5 and 1.0. */
private const val HALF_LOW: Byte = 0x00
private const val HALF_POINT_FIVE: Byte = 0x38
private const val HALF_ONE: Byte = 0x3C

/**
 * What every lit pass on one device shares for textures: the bind group layout of the texture group
 * (one texture per [TextureSlot] at its slot's number, then their samplers), and the one-texel
 * defaults for empty slots: white, and a flat normal stored exactly as half floats.
 */
internal class TextureBindings(context: GpuContext) : AutoCloseable {
  val layout: MemorySegment =
    createBindGroupLayout(
      context.device,
      TextureSlot.entries.map { LayoutEntry.Texture(Flags.STAGE_FRAGMENT) } +
        TextureSlot.entries.map { LayoutEntry.Sampler(Flags.STAGE_FRAGMENT) },
    )

  private val white =
    GpuTexture(context, 1, 1, SampledFormat.RGBA8Unorm).also {
      it.write(0, byteArrayOf(FULL, FULL, FULL, FULL))
    }

  private val flatNormal =
    GpuTexture(context, 1, 1, SampledFormat.RGBA16Float).also {
      val half = byteArrayOf(HALF_LOW, HALF_POINT_FIVE)
      val one = byteArrayOf(HALF_LOW, HALF_ONE)
      it.write(0, half + half + one + one)
    }

  /** The texture an empty [slot] samples. */
  fun default(slot: TextureSlot): GpuTexture = if (slot == TextureSlot.NORMAL) flatNormal else white

  override fun close() {
    white.close()
    flatNormal.close()
    wgpu_h.wgpuBindGroupLayoutRelease(layout)
  }
}
