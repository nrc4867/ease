package dev.chieppa.ease.gpu.ffi

import dev.chieppa.webgpu.ffi.WGPUBindGroupDescriptor
import dev.chieppa.webgpu.ffi.WGPUBindGroupEntry
import dev.chieppa.webgpu.ffi.WGPUBufferDescriptor
import dev.chieppa.webgpu.ffi.wgpu_h
import java.lang.foreign.Arena
import java.lang.foreign.MemorySegment

/** A buffer of [bytes] on [device] for [usage], a combination of [Flags]' buffer flags. */
internal fun createBuffer(device: MemorySegment, bytes: Long, usage: Long): MemorySegment =
  Arena.ofConfined().use { arena ->
    val descriptor = arena.struct(WGPUBufferDescriptor.layout())
    WGPUBufferDescriptor.size(descriptor, bytes)
    WGPUBufferDescriptor.usage(descriptor, usage)
    wgpu_h.wgpuDeviceCreateBuffer(device, descriptor)
  }

/** A whole buffer bound at one binding of a bind group. */
internal class BufferBinding(val buffer: MemorySegment, val bytes: Long)

/**
 * A bind group on [device] for group [group] of [pipeline]'s layout, binding each of [buffers] at
 * the binding numbered by its position.
 */
internal fun createBufferBindGroup(
  device: MemorySegment,
  pipeline: MemorySegment,
  buffers: List<BufferBinding>,
  group: Int = 0,
): MemorySegment {
  val layout = wgpu_h.wgpuRenderPipelineGetBindGroupLayout(pipeline, group)
  val bindGroup =
    Arena.ofConfined().use { arena ->
      val entries = arena.allocate(WGPUBindGroupEntry.layout(), buffers.size.toLong())
      buffers.forEachIndexed { index, binding ->
        val entry = WGPUBindGroupEntry.asSlice(entries, index.toLong())
        WGPUBindGroupEntry.binding(entry, index)
        WGPUBindGroupEntry.buffer(entry, binding.buffer)
        WGPUBindGroupEntry.size(entry, binding.bytes)
      }
      val descriptor = arena.struct(WGPUBindGroupDescriptor.layout())
      WGPUBindGroupDescriptor.layout(descriptor, layout)
      WGPUBindGroupDescriptor.entryCount(descriptor, buffers.size.toLong())
      WGPUBindGroupDescriptor.entries(descriptor, entries)
      wgpu_h.wgpuDeviceCreateBindGroup(device, descriptor)
    }
  wgpu_h.wgpuBindGroupLayoutRelease(layout)
  return bindGroup
}
