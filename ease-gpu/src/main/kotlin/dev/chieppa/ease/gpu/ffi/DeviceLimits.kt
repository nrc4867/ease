package dev.chieppa.ease.gpu.ffi

import dev.chieppa.ease.gpu.GpuException
import dev.chieppa.ease.gpu.GpuLimits
import dev.chieppa.webgpu.ffi.WGPULimits
import dev.chieppa.webgpu.ffi.wgpu_h
import java.lang.foreign.Arena
import java.lang.foreign.MemorySegment

/** The limits [device] was created with. */
internal fun deviceLimits(device: MemorySegment): GpuLimits =
  Arena.ofConfined().use { arena ->
    val limits = arena.struct(WGPULimits.layout())
    val status = wgpu_h.wgpuDeviceGetLimits(device, limits)
    if (status != wgpu_h.WGPUStatus_Success())
      throw GpuException("Reading the device's limits failed")
    GpuLimits(WGPULimits.maxBufferSize(limits), WGPULimits.maxStorageBufferBindingSize(limits))
  }
