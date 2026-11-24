package dev.chieppa.ease.gpu.ffi

import dev.chieppa.ease.gpu.GpuContext
import dev.chieppa.ease.gpu.GpuException
import dev.chieppa.ease.platform.PixelSize
import dev.chieppa.webgpu.ffi.WGPUBufferMapCallback
import dev.chieppa.webgpu.ffi.WGPUBufferMapCallbackInfo
import dev.chieppa.webgpu.ffi.WGPUExtent3D
import dev.chieppa.webgpu.ffi.WGPUTexelCopyBufferInfo
import dev.chieppa.webgpu.ffi.WGPUTexelCopyBufferLayout
import dev.chieppa.webgpu.ffi.WGPUTexelCopyTextureInfo
import dev.chieppa.webgpu.ffi.wgpu_h
import java.lang.foreign.Arena
import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout

/** Records a copy of all of [texture], [size] pixels, into [buffer] with rows [rowBytes] apart. */
internal fun copyTextureToBuffer(
  encoder: MemorySegment,
  texture: MemorySegment,
  buffer: MemorySegment,
  size: PixelSize,
  rowBytes: Int,
) =
  Arena.ofConfined().use { arena ->
    val source = arena.struct(WGPUTexelCopyTextureInfo.layout())
    WGPUTexelCopyTextureInfo.texture(source, texture)
    WGPUTexelCopyTextureInfo.aspect(source, wgpu_h.WGPUTextureAspect_All())
    val destination = arena.struct(WGPUTexelCopyBufferInfo.layout())
    WGPUTexelCopyBufferInfo.buffer(destination, buffer)
    val layout = WGPUTexelCopyBufferInfo.layout(destination)
    WGPUTexelCopyBufferLayout.bytesPerRow(layout, rowBytes)
    WGPUTexelCopyBufferLayout.rowsPerImage(layout, size.height)
    val extent = arena.struct(WGPUExtent3D.layout())
    WGPUExtent3D.width(extent, size.width)
    WGPUExtent3D.height(extent, size.height)
    WGPUExtent3D.depthOrArrayLayers(extent, 1)
    wgpu_h.wgpuCommandEncoderCopyTextureToBuffer(encoder, source, destination, extent)
  }

/**
 * The first [bytes] of [buffer], mapped for reading once the GPU has finished the work submitted
 * before. Blocks until then; the buffer is unmapped again before it returns.
 */
internal fun readMapped(context: GpuContext, buffer: MemorySegment, bytes: Long): ByteArray =
  Arena.ofConfined().use { arena ->
    var status: Int? = null
    var message = ""
    val info = arena.struct(WGPUBufferMapCallbackInfo.layout())
    WGPUBufferMapCallbackInfo.mode(info, wgpu_h.WGPUCallbackMode_AllowProcessEvents())
    WGPUBufferMapCallbackInfo.callback(
      info,
      WGPUBufferMapCallback.allocate(
        { result, text, _, _ ->
          status = result
          message = readString(text)
        },
        arena,
      ),
    )
    wgpu_h.wgpuBufferMapAsync(arena, buffer, Flags.MAP_READ, 0, bytes, info)
    while (status == null) context.poll(wait = true)
    if (status != wgpu_h.WGPUMapAsyncStatus_Success()) {
      throw GpuException("Reading a GPU buffer failed: $message")
    }
    val mapped = wgpu_h.wgpuBufferGetConstMappedRange(buffer, 0, bytes).reinterpret(bytes)
    val copy = mapped.toArray(ValueLayout.JAVA_BYTE)
    wgpu_h.wgpuBufferUnmap(buffer)
    copy
  }
