package dev.chieppa.ease.gpu.ffi

import dev.chieppa.webgpu.ffi.WGPUBindGroupDescriptor
import dev.chieppa.webgpu.ffi.WGPUBindGroupEntry
import dev.chieppa.webgpu.ffi.wgpu_h
import java.lang.foreign.Arena
import java.lang.foreign.MemorySegment

/** One resource bound at one binding of a bind group. */
internal sealed interface Binding {
  /** A whole buffer of [bytes]. */
  class Buffer(val buffer: MemorySegment, val bytes: Long) : Binding

  /** A texture view. */
  class Texture(val view: MemorySegment) : Binding

  /** A sampler. */
  class Sampler(val sampler: MemorySegment) : Binding
}

/**
 * A bind group on [device] for group [group] of [pipeline]'s own layout, binding each of [bindings]
 * at the binding numbered by its position.
 */
internal fun createBindGroup(
  device: MemorySegment,
  pipeline: MemorySegment,
  bindings: List<Binding>,
  group: Int,
): MemorySegment {
  val layout = wgpu_h.wgpuRenderPipelineGetBindGroupLayout(pipeline, group)
  val bindGroup = createBindGroup(device, layout, bindings)
  wgpu_h.wgpuBindGroupLayoutRelease(layout)
  return bindGroup
}

/**
 * A bind group on [device] for [layout], binding each of [bindings] at the binding numbered by its
 * position. Pipelines whose layout holds [layout] accept it, whichever made them.
 */
internal fun createBindGroup(
  device: MemorySegment,
  layout: MemorySegment,
  bindings: List<Binding>,
): MemorySegment =
  Arena.ofConfined().use { arena ->
    val entries = arena.allocate(WGPUBindGroupEntry.layout(), bindings.size.toLong())
    bindings.forEachIndexed { index, binding ->
      val entry = WGPUBindGroupEntry.asSlice(entries, index.toLong())
      WGPUBindGroupEntry.binding(entry, index)
      when (binding) {
        is Binding.Buffer -> {
          WGPUBindGroupEntry.buffer(entry, binding.buffer)
          WGPUBindGroupEntry.size(entry, binding.bytes)
        }
        is Binding.Texture -> WGPUBindGroupEntry.textureView(entry, binding.view)
        is Binding.Sampler -> WGPUBindGroupEntry.sampler(entry, binding.sampler)
      }
    }
    val descriptor = arena.struct(WGPUBindGroupDescriptor.layout())
    WGPUBindGroupDescriptor.layout(descriptor, layout)
    WGPUBindGroupDescriptor.entryCount(descriptor, bindings.size.toLong())
    WGPUBindGroupDescriptor.entries(descriptor, entries)
    wgpu_h.wgpuDeviceCreateBindGroup(device, descriptor)
  }
