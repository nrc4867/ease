package dev.chieppa.ease.gpu

import dev.chieppa.ease.gpu.ffi.Flags
import dev.chieppa.ease.gpu.ffi.copyTextureToBuffer
import dev.chieppa.ease.gpu.ffi.readMapped
import dev.chieppa.ease.platform.PixelSize
import dev.chieppa.webgpu.ffi.WGPUBufferDescriptor
import dev.chieppa.webgpu.ffi.WGPUExtent3D
import dev.chieppa.webgpu.ffi.WGPUTextureDescriptor
import dev.chieppa.webgpu.ffi.wgpu_h
import java.lang.foreign.Arena
import java.lang.foreign.MemorySegment

/** Texture copies into buffers start each row on a multiple of this many bytes. */
private const val ROW_ALIGNMENT = 256

/**
 * A texture to render into instead of a window, and read back: for headless runs, captures and
 * tests.
 */
public class OffscreenTarget(
  private val context: GpuContext,
  public val size: PixelSize,
  public val format: TextureFormat = TextureFormat.RGBA8Unorm,
) : AutoCloseable {
  init {
    require(!size.isEmpty) { "An offscreen target needs pixels; got $size" }
  }

  private val texture =
    Arena.ofConfined().use { arena ->
      val descriptor = arena.allocate(WGPUTextureDescriptor.layout())
      WGPUTextureDescriptor.usage(
        descriptor,
        Flags.TEXTURE_RENDER_ATTACHMENT or Flags.TEXTURE_COPY_SRC,
      )
      WGPUTextureDescriptor.dimension(descriptor, wgpu_h.WGPUTextureDimension_2D())
      val extent = WGPUTextureDescriptor.size(descriptor)
      WGPUExtent3D.width(extent, size.width)
      WGPUExtent3D.height(extent, size.height)
      WGPUExtent3D.depthOrArrayLayers(extent, 1)
      WGPUTextureDescriptor.format(descriptor, format.native)
      WGPUTextureDescriptor.mipLevelCount(descriptor, 1)
      WGPUTextureDescriptor.sampleCount(descriptor, 1)
      wgpu_h.wgpuDeviceCreateTexture(context.device, descriptor)
    }
  private val view = wgpu_h.wgpuTextureCreateView(texture, MemorySegment.NULL)
  private val rowBytes = paddedRowBytes(size.width)
  private val readback =
    Arena.ofConfined().use { arena ->
      val descriptor = arena.allocate(WGPUBufferDescriptor.layout())
      WGPUBufferDescriptor.size(descriptor, rowBytes.toLong() * size.height)
      WGPUBufferDescriptor.usage(descriptor, Flags.BUFFER_COPY_DST or Flags.BUFFER_MAP_READ)
      wgpu_h.wgpuDeviceCreateBuffer(context.device, descriptor)
    }

  /** The target passes draw into. */
  public val target: RenderTarget = RenderTarget(view, size, format)

  /**
   * Copies what was drawn into the target back to memory. Submits its own copy after everything
   * already submitted and blocks until the GPU has finished it.
   */
  public fun read(): Pixels {
    val encoder = CommandEncoder.on(context)
    copyTextureToBuffer(encoder.handle, texture, readback, size, rowBytes)
    encoder.submit()
    val bytes = readMapped(context, readback, rowBytes.toLong() * size.height)
    val packed = ByteArray(size.width * size.height * BYTES_PER_PIXEL)
    val packedRow = size.width * BYTES_PER_PIXEL
    for (row in 0 until size.height) {
      System.arraycopy(bytes, row * rowBytes, packed, row * packedRow, packedRow)
    }
    context.checkErrors()
    return Pixels(size, format, packed)
  }

  override fun close() {
    wgpu_h.wgpuBufferRelease(readback)
    wgpu_h.wgpuTextureViewRelease(view)
    wgpu_h.wgpuTextureRelease(texture)
  }

  private fun paddedRowBytes(width: Int): Int {
    val bytes = width * BYTES_PER_PIXEL
    return (bytes + ROW_ALIGNMENT - 1) / ROW_ALIGNMENT * ROW_ALIGNMENT
  }
}
