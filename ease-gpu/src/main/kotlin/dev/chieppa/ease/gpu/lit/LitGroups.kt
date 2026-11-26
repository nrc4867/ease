package dev.chieppa.ease.gpu.lit

import dev.chieppa.ease.gpu.GpuContext
import dev.chieppa.ease.gpu.ffi.Binding
import dev.chieppa.ease.gpu.ffi.Flags
import dev.chieppa.ease.gpu.ffi.LayoutEntry
import dev.chieppa.ease.gpu.ffi.createBindGroup
import dev.chieppa.ease.gpu.ffi.createBindGroupLayout
import dev.chieppa.ease.gpu.ffi.createPipelineLayout
import dev.chieppa.webgpu.ffi.wgpu_h
import java.lang.foreign.MemorySegment

private const val BOTH_STAGES = Flags.STAGE_VERTEX or Flags.STAGE_FRAGMENT

/**
 * A lit pass's explicit pipeline layout and the bind groups of its own buffers. Group 0 holds the
 * frame (binding 0) and the view (binding 1) uniforms, group 1 the device's texture layout, group 2
 * the materials as one read-only storage array, group 3 the objects likewise. Because the layout is
 * explicit, texture sets made on the device fit every lit pass on it, and templates may leave
 * bindings unused.
 */
@Suppress("LongParameterList") // The pass's buffers, each with its size where it is an array.
internal class LitGroups(
  context: GpuContext,
  frameBuffer: MemorySegment,
  viewBuffer: MemorySegment,
  materialBuffer: MemorySegment,
  materialBytes: Long,
  objectBuffer: MemorySegment,
  objectBytes: Long,
) : AutoCloseable {
  private val device = context.device
  private val frameLayout =
    createBindGroupLayout(
      device,
      listOf(LayoutEntry.Uniform(BOTH_STAGES), LayoutEntry.Uniform(BOTH_STAGES)),
    )
  private val materialLayout =
    createBindGroupLayout(device, listOf(LayoutEntry.ReadOnlyStorage(BOTH_STAGES)))
  private val objectLayout =
    createBindGroupLayout(device, listOf(LayoutEntry.ReadOnlyStorage(Flags.STAGE_VERTEX)))

  /** The layout every pipeline of the pass uses. */
  val pipelineLayout: MemorySegment =
    createPipelineLayout(
      device,
      listOf(frameLayout, context.textureBindings.layout, materialLayout, objectLayout),
    )

  val frame: MemorySegment =
    createBindGroup(
      device,
      frameLayout,
      listOf(
        Binding.Buffer(frameBuffer, FrameDataWriter.STRUCT_SIZE),
        Binding.Buffer(viewBuffer, ViewDataWriter.STRUCT_SIZE),
      ),
    )

  val materials: MemorySegment =
    createBindGroup(device, materialLayout, listOf(Binding.Buffer(materialBuffer, materialBytes)))

  val objects: MemorySegment =
    createBindGroup(device, objectLayout, listOf(Binding.Buffer(objectBuffer, objectBytes)))

  override fun close() {
    listOf(frame, materials, objects).forEach(wgpu_h::wgpuBindGroupRelease)
    wgpu_h.wgpuPipelineLayoutRelease(pipelineLayout)
    listOf(frameLayout, materialLayout, objectLayout).forEach(wgpu_h::wgpuBindGroupLayoutRelease)
  }

  companion object {
    const val FRAME: Int = 0
    const val TEXTURES: Int = 1
    const val MATERIALS: Int = 2
    const val OBJECTS: Int = 3
  }
}
