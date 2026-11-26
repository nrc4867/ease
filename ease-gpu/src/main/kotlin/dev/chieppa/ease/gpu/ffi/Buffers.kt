package dev.chieppa.ease.gpu.ffi

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
): MemorySegment =
  createBindGroup(device, pipeline, buffers.map { Binding.Buffer(it.buffer, it.bytes) }, group)
