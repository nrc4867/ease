package dev.chieppa.ease.gpu.ffi

import dev.chieppa.ease.gpu.Color
import dev.chieppa.ease.memory.FrameArena
import dev.chieppa.webgpu.ffi.WGPUColor
import dev.chieppa.webgpu.ffi.WGPURenderPassColorAttachment
import dev.chieppa.webgpu.ffi.WGPURenderPassDepthStencilAttachment
import dev.chieppa.webgpu.ffi.WGPURenderPassDescriptor
import dev.chieppa.webgpu.ffi.wgpu_h
import java.lang.foreign.Arena
import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout

/** webgpu.h's WGPU_DEPTH_SLICE_UNDEFINED, for a color attachment that is not a 3D texture. */
private const val DEPTH_SLICE_UNDEFINED = -1

/** The depth a depth attachment clears to: the far plane. */
private const val FAR_DEPTH = 1f

private const val COLOR_PASS_SITE = "beginColorPass"

private val COLOR_VIEW = WGPURenderPassColorAttachment.`view$offset`()
private val DEPTH_VIEW = WGPURenderPassDepthStencilAttachment.`view$offset`()

/** Points [attachment]'s view, at [offset], at the texture view at [view]. */
private fun setView(attachment: MemorySegment, offset: Long, view: Long) {
  attachment.set(ValueLayout.JAVA_LONG, offset, view)
}

/** Sets [attachment] to clear to opaque [clear] and store what is drawn. */
private fun clearingColor(attachment: MemorySegment, clear: Color) {
  WGPURenderPassColorAttachment.depthSlice(attachment, DEPTH_SLICE_UNDEFINED)
  WGPURenderPassColorAttachment.loadOp(attachment, wgpu_h.WGPULoadOp_Clear())
  WGPURenderPassColorAttachment.storeOp(attachment, wgpu_h.WGPUStoreOp_Store())
  setClear(attachment, clear)
}

private fun setClear(attachment: MemorySegment, clear: Color) {
  val color = WGPURenderPassColorAttachment.clearValue(attachment)
  WGPUColor.r(color, clear.red.toDouble())
  WGPUColor.g(color, clear.green.toDouble())
  WGPUColor.b(color, clear.blue.toDouble())
  WGPUColor.a(color, 1.0)
}

/**
 * The descriptor of a render pass that clears one color view to opaque [clear] and stores what it
 * draws, held in [arena] so a pass recorded every frame reuses it instead of allocating.
 */
internal class ColorPassDescriptor(arena: Arena, clear: Color, label: String) {
  private val attachment = arena.struct(WGPURenderPassColorAttachment.layout())
  private val descriptor = arena.struct(WGPURenderPassDescriptor.layout())

  init {
    clearingColor(attachment, clear)
    arena.setString(WGPURenderPassDescriptor.label(descriptor), label)
    WGPURenderPassDescriptor.colorAttachmentCount(descriptor, 1)
    WGPURenderPassDescriptor.colorAttachments(descriptor, attachment)
  }

  /** Begins the pass on [encoder], drawing into [view]; both are addresses. */
  fun begin(encoder: Long, view: Long): Long {
    setView(attachment, COLOR_VIEW, view)
    return FrameCalls.beginRenderPass(encoder, descriptor)
  }
}

/**
 * The descriptor of a render pass that clears one color view and a depth view, then stores the
 * color and discards the depth, held in [arena] so a pass recorded every frame reuses it.
 */
internal class DepthPassDescriptor(arena: Arena, label: String) {
  private val color = arena.struct(WGPURenderPassColorAttachment.layout())
  private val depth = arena.struct(WGPURenderPassDepthStencilAttachment.layout())
  private val descriptor = arena.struct(WGPURenderPassDescriptor.layout())

  /** The clear color [color] holds; set again only when it changes, as setting it allocates. */
  private var cleared = Color(0f, 0f, 0f)

  init {
    clearingColor(color, cleared)
    WGPURenderPassDepthStencilAttachment.depthLoadOp(depth, wgpu_h.WGPULoadOp_Clear())
    WGPURenderPassDepthStencilAttachment.depthStoreOp(depth, wgpu_h.WGPUStoreOp_Discard())
    WGPURenderPassDepthStencilAttachment.depthClearValue(depth, FAR_DEPTH)
    arena.setString(WGPURenderPassDescriptor.label(descriptor), label)
    WGPURenderPassDescriptor.colorAttachmentCount(descriptor, 1)
    WGPURenderPassDescriptor.colorAttachments(descriptor, color)
    WGPURenderPassDescriptor.depthStencilAttachment(descriptor, depth)
  }

  /**
   * Begins the pass on [encoder], clearing [colorView] to [clear] and drawing into it with
   * [depthView] as its depth buffer; all three are addresses.
   */
  fun begin(encoder: Long, colorView: Long, depthView: Long, clear: Color): Long {
    setView(color, COLOR_VIEW, colorView)
    if (clear != cleared) {
      setClear(color, clear)
      cleared = clear
    }
    setView(depth, DEPTH_VIEW, depthView)
    return FrameCalls.beginRenderPass(encoder, descriptor)
  }
}

/**
 * Begins a render pass on [encoder] that clears [view] to opaque [clear] and stores what it draws,
 * its descriptor written into [frames].
 */
internal fun beginColorPass(frames: FrameArena, encoder: Long, view: Long, clear: Color): Long {
  val attachment = frames.take(WGPURenderPassColorAttachment.layout().byteSize(), COLOR_PASS_SITE)
  val descriptor = frames.take(WGPURenderPassDescriptor.layout().byteSize(), COLOR_PASS_SITE)
  val color = attachment.segment
  val pass = descriptor.segment
  clearingColor(color, clear)
  setView(color, COLOR_VIEW, view)
  WGPURenderPassDescriptor.colorAttachmentCount(pass, 1)
  WGPURenderPassDescriptor.colorAttachments(pass, color)
  return FrameCalls.beginRenderPass(encoder, pass)
}
