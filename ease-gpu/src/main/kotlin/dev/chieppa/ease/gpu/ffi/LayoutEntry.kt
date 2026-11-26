package dev.chieppa.ease.gpu.ffi

import dev.chieppa.webgpu.ffi.WGPUBindGroupLayoutDescriptor
import dev.chieppa.webgpu.ffi.WGPUBindGroupLayoutEntry
import dev.chieppa.webgpu.ffi.WGPUBufferBindingLayout
import dev.chieppa.webgpu.ffi.WGPUPipelineLayoutDescriptor
import dev.chieppa.webgpu.ffi.WGPUSamplerBindingLayout
import dev.chieppa.webgpu.ffi.WGPUTextureBindingLayout
import dev.chieppa.webgpu.ffi.wgpu_h
import java.lang.foreign.Arena
import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout

/** What one binding of a bind group layout holds, and which shader stages see it. */
internal sealed interface LayoutEntry {
  /** The stages that read it, a combination of [Flags]' stage flags. */
  val visibility: Long

  /** A uniform buffer. */
  class Uniform(override val visibility: Long) : LayoutEntry

  /** A storage buffer shaders only read. */
  class ReadOnlyStorage(override val visibility: Long) : LayoutEntry

  /** A filterable float 2D texture. */
  class Texture(override val visibility: Long) : LayoutEntry

  /** A filtering sampler. */
  class Sampler(override val visibility: Long) : LayoutEntry
}

/** A bind group layout on [device] with each of [entries] at the binding numbered by position. */
internal fun createBindGroupLayout(
  device: MemorySegment,
  entries: List<LayoutEntry>,
): MemorySegment =
  Arena.ofConfined().use { arena ->
    val native = arena.allocate(WGPUBindGroupLayoutEntry.layout(), entries.size.toLong())
    entries.forEachIndexed { index, entry ->
      val slot = WGPUBindGroupLayoutEntry.asSlice(native, index.toLong())
      WGPUBindGroupLayoutEntry.binding(slot, index)
      WGPUBindGroupLayoutEntry.visibility(slot, entry.visibility)
      when (entry) {
        is LayoutEntry.Uniform ->
          WGPUBufferBindingLayout.type(
            WGPUBindGroupLayoutEntry.buffer(slot),
            wgpu_h.WGPUBufferBindingType_Uniform(),
          )
        is LayoutEntry.ReadOnlyStorage ->
          WGPUBufferBindingLayout.type(
            WGPUBindGroupLayoutEntry.buffer(slot),
            wgpu_h.WGPUBufferBindingType_ReadOnlyStorage(),
          )
        is LayoutEntry.Texture -> {
          val texture = WGPUBindGroupLayoutEntry.texture(slot)
          WGPUTextureBindingLayout.sampleType(texture, wgpu_h.WGPUTextureSampleType_Float())
          WGPUTextureBindingLayout.viewDimension(texture, wgpu_h.WGPUTextureViewDimension_2D())
        }
        is LayoutEntry.Sampler ->
          WGPUSamplerBindingLayout.type(
            WGPUBindGroupLayoutEntry.sampler(slot),
            wgpu_h.WGPUSamplerBindingType_Filtering(),
          )
      }
    }
    val descriptor = arena.struct(WGPUBindGroupLayoutDescriptor.layout())
    WGPUBindGroupLayoutDescriptor.entryCount(descriptor, entries.size.toLong())
    WGPUBindGroupLayoutDescriptor.entries(descriptor, native)
    wgpu_h.wgpuDeviceCreateBindGroupLayout(device, descriptor)
  }

/** A pipeline layout on [device] whose group n is [groups]' nth layout. */
internal fun createPipelineLayout(
  device: MemorySegment,
  groups: List<MemorySegment>,
): MemorySegment =
  Arena.ofConfined().use { arena ->
    val layouts = arena.allocate(ValueLayout.ADDRESS, groups.size.toLong())
    groups.forEachIndexed { index, layout ->
      layouts.setAtIndex(ValueLayout.ADDRESS, index.toLong(), layout)
    }
    val descriptor = arena.struct(WGPUPipelineLayoutDescriptor.layout())
    WGPUPipelineLayoutDescriptor.bindGroupLayoutCount(descriptor, groups.size.toLong())
    WGPUPipelineLayoutDescriptor.bindGroupLayouts(descriptor, layouts)
    wgpu_h.wgpuDeviceCreatePipelineLayout(device, descriptor)
  }
