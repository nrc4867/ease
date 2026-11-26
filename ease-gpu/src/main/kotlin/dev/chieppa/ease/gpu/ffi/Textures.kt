package dev.chieppa.ease.gpu.ffi

import dev.chieppa.ease.platform.PixelSize
import dev.chieppa.webgpu.ffi.WGPUExtent3D
import dev.chieppa.webgpu.ffi.WGPUTexelCopyBufferLayout
import dev.chieppa.webgpu.ffi.WGPUTexelCopyTextureInfo
import dev.chieppa.webgpu.ffi.WGPUTextureDescriptor
import dev.chieppa.webgpu.ffi.wgpu_h
import java.lang.foreign.Arena
import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout

/** The format of every depth buffer Ease draws with. */
internal val DEPTH_FORMAT: Int = wgpu_h.WGPUTextureFormat_Depth32Float()

/** A single-sampled 2D texture of [size] in [format] on [device], for [usage], with [mipLevels]. */
internal fun createTexture(
  device: MemorySegment,
  size: PixelSize,
  format: Int,
  usage: Long,
  mipLevels: Int = 1,
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
    WGPUTextureDescriptor.mipLevelCount(descriptor, mipLevels)
    WGPUTextureDescriptor.sampleCount(descriptor, 1)
    wgpu_h.wgpuDeviceCreateTexture(device, descriptor)
  }

/**
 * Copies [data] into mip level [level] of [texture] through [queue]: [rows] rows of blocks (or of
 * texels, for uncompressed formats), each [rowBytes] long, covering [size] texels.
 */
@Suppress("LongParameterList") // One texture copy, as wgpu takes it.
internal fun writeTextureLevel(
  queue: MemorySegment,
  texture: MemorySegment,
  level: Int,
  size: PixelSize,
  data: ByteArray,
  rowBytes: Int,
  rows: Int,
) =
  Arena.ofConfined().use { arena ->
    val destination = arena.struct(WGPUTexelCopyTextureInfo.layout())
    WGPUTexelCopyTextureInfo.texture(destination, texture)
    WGPUTexelCopyTextureInfo.mipLevel(destination, level)
    WGPUTexelCopyTextureInfo.aspect(destination, wgpu_h.WGPUTextureAspect_All())
    val layout = arena.struct(WGPUTexelCopyBufferLayout.layout())
    WGPUTexelCopyBufferLayout.bytesPerRow(layout, rowBytes)
    WGPUTexelCopyBufferLayout.rowsPerImage(layout, rows)
    val extent = arena.struct(WGPUExtent3D.layout())
    WGPUExtent3D.width(extent, size.width)
    WGPUExtent3D.height(extent, size.height)
    WGPUExtent3D.depthOrArrayLayers(extent, 1)
    val bytes = arena.allocate(data.size.toLong())
    MemorySegment.copy(data, 0, bytes, ValueLayout.JAVA_BYTE, 0, data.size)
    wgpu_h.wgpuQueueWriteTexture(queue, destination, bytes, data.size.toLong(), layout, extent)
  }
