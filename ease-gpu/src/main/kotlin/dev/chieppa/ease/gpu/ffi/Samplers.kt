package dev.chieppa.ease.gpu.ffi

import dev.chieppa.webgpu.ffi.WGPUSamplerDescriptor
import dev.chieppa.webgpu.ffi.wgpu_h
import java.lang.foreign.Arena
import java.lang.foreign.MemorySegment

/** The most levels a sampler reads: every level of any texture. */
private const val ALL_LEVELS = 32f

/**
 * A sampler on [device]: [linear] blends texels and mip levels (nearest takes one of each), and
 * [addressMode], a webgpu.h address mode, applies on both axes.
 */
internal fun createSampler(
  device: MemorySegment,
  linear: Boolean,
  addressMode: Int,
): MemorySegment =
  Arena.ofConfined().use { arena ->
    val descriptor = arena.struct(WGPUSamplerDescriptor.layout())
    WGPUSamplerDescriptor.addressModeU(descriptor, addressMode)
    WGPUSamplerDescriptor.addressModeV(descriptor, addressMode)
    WGPUSamplerDescriptor.addressModeW(descriptor, addressMode)
    val filter = if (linear) wgpu_h.WGPUFilterMode_Linear() else wgpu_h.WGPUFilterMode_Nearest()
    WGPUSamplerDescriptor.magFilter(descriptor, filter)
    WGPUSamplerDescriptor.minFilter(descriptor, filter)
    WGPUSamplerDescriptor.mipmapFilter(
      descriptor,
      if (linear) wgpu_h.WGPUMipmapFilterMode_Linear() else wgpu_h.WGPUMipmapFilterMode_Nearest(),
    )
    WGPUSamplerDescriptor.lodMinClamp(descriptor, 0f)
    WGPUSamplerDescriptor.lodMaxClamp(descriptor, ALL_LEVELS)
    WGPUSamplerDescriptor.maxAnisotropy(descriptor, 1.toShort())
    wgpu_h.wgpuDeviceCreateSampler(device, descriptor)
  }
