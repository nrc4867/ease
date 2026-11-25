package dev.chieppa.ease.gpu.ffi

import dev.chieppa.ease.platform.PixelSize
import dev.chieppa.webgpu.ffi.WGPUExtent3D
import dev.chieppa.webgpu.ffi.WGPUTextureDescriptor
import dev.chieppa.webgpu.ffi.wgpu_h
import java.lang.foreign.Arena
import java.lang.foreign.MemorySegment

/** The format of every depth buffer Ease draws with. */
internal val DEPTH_FORMAT: Int = wgpu_h.WGPUTextureFormat_Depth32Float()

/** A single-sampled 2D texture of [size] in [format] on [device], for [usage]. */
internal fun createTexture(
  device: MemorySegment,
  size: PixelSize,
  format: Int,
  usage: Long,
): MemorySegment =
  Arena.ofConfined().use { arena ->
    val descriptor = arena.struct(WGPUTextureDescriptor.layout())
    WGPUTextureDescriptor.usage(descriptor, usage)
    WGPUTextureDescriptor.dimension(descriptor, wgpu_h.WGPUTextureDimension_2D())
    val extent = WGPUTextureDescriptor.size(descriptor)
    WGPUExtent3D.width(extent, size.width)
    WGPUExtent3D.height(extent, size.height)
    WGPUExtent3D.depthOrArrayLayers(extent, 1)
    WGPUTextureDescriptor.format(descriptor, format)
    WGPUTextureDescriptor.mipLevelCount(descriptor, 1)
    WGPUTextureDescriptor.sampleCount(descriptor, 1)
    wgpu_h.wgpuDeviceCreateTexture(device, descriptor)
  }
