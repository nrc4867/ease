package dev.chieppa.ease.gpu.lit

import dev.chieppa.ease.gpu.GpuContext
import dev.chieppa.ease.gpu.ffi.DEPTH_FORMAT
import dev.chieppa.ease.gpu.ffi.Flags
import dev.chieppa.ease.gpu.ffi.createTexture
import dev.chieppa.ease.platform.PixelSize
import dev.chieppa.webgpu.ffi.wgpu_h
import java.lang.foreign.MemorySegment

/** A depth texture matching a target's size, made again only when that size changes. */
internal class DepthBuffer(private val context: GpuContext) : AutoCloseable {
  private var texture = MemorySegment.NULL
  private var view = MemorySegment.NULL
  private var width = 0
  private var height = 0

  /** The address of the depth view for a target of [size]. */
  fun viewFor(size: PixelSize): Long {
    if (size.width != width || size.height != height) {
      close()
      texture = createTexture(context.device, size, DEPTH_FORMAT, Flags.TEXTURE_RENDER_ATTACHMENT)
      view = wgpu_h.wgpuTextureCreateView(texture, MemorySegment.NULL)
      width = size.width
      height = size.height
    }
    return view.address()
  }

  override fun close() {
    if (view != MemorySegment.NULL) wgpu_h.wgpuTextureViewRelease(view)
    if (texture != MemorySegment.NULL) wgpu_h.wgpuTextureRelease(texture)
    view = MemorySegment.NULL
    texture = MemorySegment.NULL
    width = 0
    height = 0
  }
}
