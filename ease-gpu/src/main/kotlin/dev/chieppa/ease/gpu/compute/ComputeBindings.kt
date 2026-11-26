package dev.chieppa.ease.gpu.compute

import dev.chieppa.webgpu.ffi.wgpu_h
import java.lang.foreign.MemorySegment

/** A bind group made by [ComputeKernel.bind]. Close it when no dispatch needs it any more. */
public class ComputeBindings internal constructor(internal val handle: MemorySegment) :
  AutoCloseable {
  override fun close() {
    wgpu_h.wgpuBindGroupRelease(handle)
  }
}
