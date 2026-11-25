package dev.chieppa.ease.gpu.compute

import dev.chieppa.ease.gpu.CommandEncoder
import dev.chieppa.ease.gpu.GpuContext
import dev.chieppa.ease.gpu.ffi.BufferBinding
import dev.chieppa.ease.gpu.ffi.createComputeBindGroup
import dev.chieppa.ease.gpu.ffi.createComputePipeline
import dev.chieppa.ease.gpu.ffi.createShaderModule
import dev.chieppa.ease.gpu.ffi.recordDispatch
import dev.chieppa.webgpu.ffi.wgpu_h
import java.lang.foreign.MemorySegment

/**
 * A compute shader's entry point, compiled from [wgsl] on [context]'s device. Its bind group
 * layouts are the ones wgpu derives from the shader, so a group binds exactly the bindings the
 * entry point uses, numbered from 0 in order. Building one compiles a pipeline: load-time work.
 */
public class ComputeKernel(
  private val context: GpuContext,
  wgsl: String,
  entryPoint: String,
  private val label: String = entryPoint,
) : AutoCloseable {
  private val shader = createShaderModule(context.device, wgsl, label)
  private val pipeline =
    createComputePipeline(context.device, shader, entryPoint, label).also {
      context.checkErrors()
    }

  /** A bind group for [group] of this kernel, binding [buffers] at bindings 0, 1, 2 and on. */
  public fun bind(group: Int, buffers: List<GpuBuffer>): ComputeBindings {
    val bindings = buffers.map { BufferBinding(it.handle, it.bytes) }
    val handle = createComputeBindGroup(context.device, pipeline, bindings, group)
    context.checkErrors()
    return ComputeBindings(handle)
  }

  /**
   * Records into [encoder] a pass running this kernel over [x] by [y] by [z] workgroups, with
   * [groups] set at the bind groups numbered by their positions.
   */
  public fun dispatch(
    encoder: CommandEncoder,
    groups: List<ComputeBindings>,
    x: Int,
    y: Int = 1,
    z: Int = 1,
  ) {
    require(x in 1..MAX_WORKGROUPS && y in 1..MAX_WORKGROUPS && z in 1..MAX_WORKGROUPS) {
      "$label: $x x $y x $z workgroups; each count is 1 to $MAX_WORKGROUPS"
    }
    recordDispatch(encoder.handle, pipeline, groups.map { it.handle }, x, y, z)
  }

  override fun close() {
    wgpu_h.wgpuComputePipelineRelease(pipeline)
    wgpu_h.wgpuShaderModuleRelease(shader)
  }

  public companion object {
    /** The most workgroups one dimension of a dispatch may have, WebGPU's default limit. */
    public const val MAX_WORKGROUPS: Int = 65_535
  }
}

/** A bind group made by [ComputeKernel.bind]. Close it when no dispatch needs it any more. */
public class ComputeBindings internal constructor(internal val handle: MemorySegment) :
  AutoCloseable {
  override fun close() {
    wgpu_h.wgpuBindGroupRelease(handle)
  }
}
