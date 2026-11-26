package dev.chieppa.ease.gpu.ffi

import dev.chieppa.webgpu.ffi.WGPUBindGroupDescriptor
import dev.chieppa.webgpu.ffi.WGPUBindGroupEntry
import dev.chieppa.webgpu.ffi.WGPUComputePipelineDescriptor
import dev.chieppa.webgpu.ffi.WGPUComputeState
import dev.chieppa.webgpu.ffi.wgpu_h
import java.lang.foreign.Arena
import java.lang.foreign.MemorySegment

/**
 * A compute pipeline running [shader]'s [entryPoint], with a layout wgpu derives from the shader.
 */
internal fun createComputePipeline(
  device: MemorySegment,
  shader: MemorySegment,
  entryPoint: String,
  label: String,
): MemorySegment =
  Arena.ofConfined().use { arena ->
    val descriptor = arena.struct(WGPUComputePipelineDescriptor.layout())
    arena.setString(WGPUComputePipelineDescriptor.label(descriptor), label)
    val compute = WGPUComputePipelineDescriptor.compute(descriptor)
    WGPUComputeState.module(compute, shader)
    arena.setString(WGPUComputeState.entryPoint(compute), entryPoint)
    wgpu_h.wgpuDeviceCreateComputePipeline(device, descriptor)
  }

/**
 * A bind group on [device] for group [group] of the compute [pipeline]'s layout, binding each of
 * [buffers] at the binding numbered by its position.
 */
internal fun createComputeBindGroup(
  device: MemorySegment,
  pipeline: MemorySegment,
  buffers: List<BufferBinding>,
  group: Int,
): MemorySegment {
  val layout = wgpu_h.wgpuComputePipelineGetBindGroupLayout(pipeline, group)
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

/**
 * Records into [encoder] one compute pass running [pipeline] over [x] by [y] by [z] workgroups,
 * with [bindGroups] set at the groups numbered by their positions.
 */
@Suppress("LongParameterList") // A dispatch's pipeline, bind groups and three counts.
internal fun recordDispatch(
  encoder: MemorySegment,
  pipeline: MemorySegment,
  bindGroups: List<MemorySegment>,
  x: Int,
  y: Int,
  z: Int,
) {
  val pass = wgpu_h.wgpuCommandEncoderBeginComputePass(encoder, MemorySegment.NULL)
  wgpu_h.wgpuComputePassEncoderSetPipeline(pass, pipeline)
  bindGroups.forEachIndexed { group, bindGroup ->
    wgpu_h.wgpuComputePassEncoderSetBindGroup(pass, group, bindGroup, 0, MemorySegment.NULL)
  }
  wgpu_h.wgpuComputePassEncoderDispatchWorkgroups(pass, x, y, z)
  wgpu_h.wgpuComputePassEncoderEnd(pass)
  wgpu_h.wgpuComputePassEncoderRelease(pass)
}
