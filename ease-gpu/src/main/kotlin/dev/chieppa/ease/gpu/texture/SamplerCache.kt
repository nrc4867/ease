package dev.chieppa.ease.gpu.texture

import dev.chieppa.ease.gpu.ffi.createSampler
import dev.chieppa.webgpu.ffi.wgpu_h
import java.lang.foreign.MemorySegment

/** One sampler per filter and wrap on a device, each made the first time it is asked for. */
internal class SamplerCache(private val device: MemorySegment) : AutoCloseable {
  private val samplers =
    arrayOfNulls<MemorySegment>(SamplerFilter.entries.size * SamplerWrap.entries.size)

  /** The sampler that reads with [filter] and [wrap]. */
  fun of(filter: SamplerFilter, wrap: SamplerWrap): MemorySegment {
    val slot = filter.ordinal * SamplerWrap.entries.size + wrap.ordinal
    return samplers[slot]
      ?: createSampler(device, filter == SamplerFilter.LINEAR, addressMode(wrap)).also {
        samplers[slot] = it
      }
  }

  private fun addressMode(wrap: SamplerWrap): Int =
    when (wrap) {
      SamplerWrap.REPEAT -> wgpu_h.WGPUAddressMode_Repeat()
      SamplerWrap.CLAMP -> wgpu_h.WGPUAddressMode_ClampToEdge()
      SamplerWrap.MIRROR -> wgpu_h.WGPUAddressMode_MirrorRepeat()
    }

  override fun close() {
    samplers.forEach { it?.let(wgpu_h::wgpuSamplerRelease) }
    samplers.fill(null)
  }
}
