package dev.chieppa.ease.gpu.ffi

import dev.chieppa.ease.gpu.Color
import dev.chieppa.webgpu.ffi.WGPUColor
import dev.chieppa.webgpu.ffi.WGPURenderPassColorAttachment
import dev.chieppa.webgpu.ffi.WGPURenderPassDescriptor
import dev.chieppa.webgpu.ffi.wgpu_h
import java.lang.foreign.Arena
import java.lang.foreign.MemorySegment

/** webgpu.h's WGPU_DEPTH_SLICE_UNDEFINED, for a color attachment that is not a 3D texture. */
private const val DEPTH_SLICE_UNDEFINED = -1

/**
 * The descriptor of a render pass that clears one color view to opaque [clear] and stores what it
 * draws, held in [arena] so a pass recorded every frame reuses it instead of allocating.
 */
internal class ColorPassDescriptor(arena: Arena, clear: Color, label: String) {
  private val attachment = arena.struct(WGPURenderPassColorAttachment.layout())
  private val descriptor = arena.struct(WGPURenderPassDescriptor.layout())

  init {
    WGPURenderPassColorAttachment.depthSlice(attachment, DEPTH_SLICE_UNDEFINED)
    WGPURenderPassColorAttachment.loadOp(attachment, wgpu_h.WGPULoadOp_Clear())
    WGPURenderPassColorAttachment.storeOp(attachment, wgpu_h.WGPUStoreOp_Store())
    val color = WGPURenderPassColorAttachment.clearValue(attachment)
    WGPUColor.r(color, clear.red.toDouble())
    WGPUColor.g(color, clear.green.toDouble())
    WGPUColor.b(color, clear.blue.toDouble())
    WGPUColor.a(color, 1.0)
    arena.setString(WGPURenderPassDescriptor.label(descriptor), label)
    WGPURenderPassDescriptor.colorAttachmentCount(descriptor, 1)
    WGPURenderPassDescriptor.colorAttachments(descriptor, attachment)
  }

  /** Begins the pass on [encoder], drawing into [view]. */
  fun begin(encoder: MemorySegment, view: MemorySegment): MemorySegment {
    WGPURenderPassColorAttachment.view(attachment, view)
    return wgpu_h.wgpuCommandEncoderBeginRenderPass(encoder, descriptor)
  }
}

/**
 * Begins a render pass on [encoder] that clears [view] to opaque [clear] and stores what it draws.
 */
internal fun beginColorPass(
  encoder: MemorySegment,
  view: MemorySegment,
  clear: Color,
  label: String,
): MemorySegment =
  Arena.ofConfined().use { arena -> ColorPassDescriptor(arena, clear, label).begin(encoder, view) }
