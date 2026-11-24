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
 * Begins a render pass on [encoder] that clears [view] to opaque [clear] and stores what it draws.
 */
internal fun beginColorPass(
  encoder: MemorySegment,
  view: MemorySegment,
  clear: Color,
  label: String,
): MemorySegment =
  Arena.ofConfined().use { arena ->
    val attachment = arena.struct(WGPURenderPassColorAttachment.layout())
    WGPURenderPassColorAttachment.view(attachment, view)
    WGPURenderPassColorAttachment.depthSlice(attachment, DEPTH_SLICE_UNDEFINED)
    WGPURenderPassColorAttachment.loadOp(attachment, wgpu_h.WGPULoadOp_Clear())
    WGPURenderPassColorAttachment.storeOp(attachment, wgpu_h.WGPUStoreOp_Store())
    val color = WGPURenderPassColorAttachment.clearValue(attachment)
    WGPUColor.r(color, clear.red.toDouble())
    WGPUColor.g(color, clear.green.toDouble())
    WGPUColor.b(color, clear.blue.toDouble())
    WGPUColor.a(color, 1.0)
    val pass = arena.struct(WGPURenderPassDescriptor.layout())
    arena.setString(WGPURenderPassDescriptor.label(pass), label)
    WGPURenderPassDescriptor.colorAttachmentCount(pass, 1)
    WGPURenderPassDescriptor.colorAttachments(pass, attachment)
    wgpu_h.wgpuCommandEncoderBeginRenderPass(encoder, pass)
  }
