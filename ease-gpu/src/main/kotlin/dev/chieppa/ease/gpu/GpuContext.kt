package dev.chieppa.ease.gpu

import dev.chieppa.ease.gpu.ffi.createInstance
import dev.chieppa.ease.gpu.ffi.createSurface
import dev.chieppa.ease.gpu.ffi.readString
import dev.chieppa.ease.gpu.ffi.requestAdapter
import dev.chieppa.ease.gpu.ffi.requestDevice
import dev.chieppa.ease.platform.SurfaceHandle
import dev.chieppa.webgpu.WgpuNative
import dev.chieppa.webgpu.ffi.WGPUAdapterInfo
import dev.chieppa.webgpu.ffi.wgpu_h
import java.lang.foreign.Arena
import java.lang.foreign.MemorySegment

/**
 * A wgpu device with the instance and adapter it came from, and the window surface it presents to
 * when it has one. Use it from one thread at a time.
 */
public class GpuContext
private constructor(
  internal val instance: MemorySegment,
  private val adapter: MemorySegment?,
  internal val device: MemorySegment,
  /** The GPU's name and the API underneath, such as "Example GPU (Vulkan)". */
  public val description: String,
  surfaceHandle: MemorySegment?,
  private val errors: ErrorLog,
) : AutoCloseable {
  internal val queue: MemorySegment = wgpu_h.wgpuDeviceGetQueue(device)

  /** The surface of the window this context was opened for, or null when it has none. */
  public val surface: WindowSurface? = surfaceHandle?.let {
    WindowSurface(this, it, checkNotNull(adapter))
  }

  /** Throws the first error wgpu reported for this device outside a call, if there was one. */
  public fun checkErrors() {
    errors.first?.let { throw GpuException(it) }
  }

  /** Blocks until the GPU has finished everything submitted on this device. */
  public fun awaitIdle() {
    poll(wait = true)
  }

  /** Runs wgpu's callbacks that are due; with [wait], first blocks until submitted work is done. */
  internal fun poll(wait: Boolean) {
    wgpu_h.wgpuDevicePoll(device, if (wait) 1 else 0, MemorySegment.NULL)
    wgpu_h.wgpuInstanceProcessEvents(instance)
  }

  override fun close() {
    surface?.close()
    wgpu_h.wgpuQueueRelease(queue)
    wgpu_h.wgpuDeviceRelease(device)
    adapter?.let(wgpu_h::wgpuAdapterRelease)
    wgpu_h.wgpuInstanceRelease(instance)
    errors.close()
  }

  public companion object {
    /**
     * A new device on the best adapter, able to present to [window] when one is given; that
     * window's surface is [surface].
     */
    public fun open(window: SurfaceHandle? = null): GpuContext {
      WgpuNative.load()
      val instance = createInstance()
      val surface = window?.let { createSurface(instance, it) }
      val adapter = requestAdapter(instance, surface)
      val errors = ErrorLog(Arena.ofShared())
      val device = requestDevice(instance, adapter, checkNotNull(errors.callbacks), errors::report)
      return GpuContext(instance, adapter, device, describe(adapter), surface, errors)
    }

    /**
     * A context on a device someone else made, such as an editor's viewport, given as wgpu handle
     * addresses, with the owner's [description] of it. It holds its own references, so closing it
     * leaves the owner's device working. It has no window surface, and errors go to the owner's
     * callbacks, so [checkErrors] never throws.
     */
    public fun shared(instance: Long, device: Long, description: String): GpuContext {
      WgpuNative.load()
      val instanceHandle = MemorySegment.ofAddress(instance)
      val deviceHandle = MemorySegment.ofAddress(device)
      wgpu_h.wgpuInstanceAddRef(instanceHandle)
      wgpu_h.wgpuDeviceAddRef(deviceHandle)
      return GpuContext(
        instanceHandle,
        null,
        deviceHandle,
        description,
        null,
        ErrorLog(callbacks = null),
      )
    }

    private fun describe(adapter: MemorySegment): String =
      Arena.ofConfined().use { arena ->
        val info = arena.allocate(WGPUAdapterInfo.layout())
        wgpu_h.wgpuAdapterGetInfo(adapter, info)
        val name =
          listOf(WGPUAdapterInfo.device(info), WGPUAdapterInfo.description(info))
            .map(::readString)
            .firstOrNull { it.isNotBlank() } ?: "Unknown GPU"
        val backend = backendName(WGPUAdapterInfo.backendType(info))
        wgpu_h.wgpuAdapterInfoFreeMembers(info)
        "$name ($backend)"
      }

    private fun backendName(type: Int): String =
      when (type) {
        wgpu_h.WGPUBackendType_Vulkan() -> "Vulkan"
        wgpu_h.WGPUBackendType_D3D12() -> "Direct3D 12"
        wgpu_h.WGPUBackendType_Metal() -> "Metal"
        wgpu_h.WGPUBackendType_OpenGL(),
        wgpu_h.WGPUBackendType_OpenGLES() -> "OpenGL"
        else -> "other backend"
      }
  }
}

/** An error wgpu reported for a device. */
public class GpuException(message: String) : RuntimeException(message)

/**
 * Keeps the first error wgpu reports, from whichever thread it reports on, and the [callbacks]
 * arena the device's error callbacks live in, when this context made them.
 */
private class ErrorLog(val callbacks: Arena?) : AutoCloseable {
  @Volatile
  var first: String? = null
    private set

  @Synchronized
  fun report(message: String) {
    if (first == null) first = message
  }

  override fun close() {
    callbacks?.close()
  }
}
